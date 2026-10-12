$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path (Join-Path $root 'src'))) {
    $root = 'C:\Users\nuran\IdeaProjects\obsidianwars'
}

function Convert-ColorBytes([string]$path) {
    $bytes = [System.IO.File]::ReadAllBytes($path)
    $out = New-Object System.Collections.Generic.List[byte]
    for ($i = 0; $i -lt $bytes.Length; $i++) {
        $b = $bytes[$i]
        $next = if ($i + 1 -lt $bytes.Length) { $bytes[$i + 1] } else { -1 }
        # UTF-8 section sign C2 A7 -> &
        if ($b -eq 0xC2 -and $next -eq 0xA7) {
            $out.Add(0x26)
            $i++
            continue
        }
        # Orphaned C2 left before '&' (botched prior § replacement) -> keep '&'
        if ($b -eq 0xC2 -and $next -eq 0x26) {
            continue
        }
        $out.Add($b)
    }
    [System.IO.File]::WriteAllBytes($path, $out.ToArray())
}

function Find-MatchingClose([string]$text, [int]$openIndex) {
    $depth = 0
    $inString = $false
    $escape = $false
    for ($i = $openIndex; $i -lt $text.Length; $i++) {
        $ch = $text[$i]
        if ($inString) {
            if ($escape) { $escape = $false; continue }
            if ($ch -eq '\') { $escape = $true; continue }
            if ($ch -eq '"') { $inString = $false }
            continue
        }
        if ($ch -eq '"') { $inString = $true; continue }
        if ($ch -eq '(') { $depth++ }
        elseif ($ch -eq ')') {
            $depth--
            if ($depth -eq 0) { return $i }
        }
    }
    return -1
}

function Wrap-CallArgs([string]$text, [string]$method, [scriptblock]$wrapArgs) {
    $needle = '.' + $method + '('
    $searchFrom = 0
    $sb = New-Object System.Text.StringBuilder
    while ($true) {
        $idx = $text.IndexOf($needle, $searchFrom)
        if ($idx -lt 0) {
            [void]$sb.Append($text.Substring($searchFrom))
            break
        }
        [void]$sb.Append($text.Substring($searchFrom, $idx - $searchFrom))
        $open = $idx + $needle.Length - 1
        $close = Find-MatchingClose $text $open
        if ($close -lt 0) {
            [void]$sb.Append($text.Substring($idx))
            break
        }
        $argsText = $text.Substring($open + 1, $close - $open - 1)
        $prefix = $text.Substring($idx, $open - $idx + 1)
        if ($argsText.TrimStart().StartsWith('ColorUtils.translate(') -and ($argsText.ToCharArray() | Where-Object { $_ -eq '(' }).Count -eq ($argsText.ToCharArray() | Where-Object { $_ -eq ')' }).Count) {
            # still wrap if multiple args; wrapArgs decides
        }
        $newArgs = & $wrapArgs $argsText
        [void]$sb.Append($prefix)
        [void]$sb.Append($newArgs)
        [void]$sb.Append(')')
        $searchFrom = $close + 1
    }
    return $sb.ToString()
}

function Wrap-Translate([string]$expr) {
    $trim = $expr.Trim()
    if ($trim.Length -eq 0) { return $expr }
    if ($trim.StartsWith('ColorUtils.translate(')) { return $expr }
    return 'ColorUtils.translate(' + $expr + ')'
}

function Split-TopLevelArgs([string]$argsText) {
    $args = New-Object System.Collections.Generic.List[string]
    $depth = 0
    $inString = $false
    $escape = $false
    $start = 0
    for ($i = 0; $i -lt $argsText.Length; $i++) {
        $ch = $argsText[$i]
        if ($inString) {
            if ($escape) { $escape = $false; continue }
            if ($ch -eq '\') { $escape = $true; continue }
            if ($ch -eq '"') { $inString = $false }
            continue
        }
        if ($ch -eq '"') { $inString = $true; continue }
        if ($ch -eq '(' -or $ch -eq '{') { $depth++ }
        elseif ($ch -eq ')' -or $ch -eq '}') { $depth-- }
        elseif ($ch -eq ',' -and $depth -eq 0) {
            $args.Add($argsText.Substring($start, $i - $start))
            $start = $i + 1
        }
    }
    $args.Add($argsText.Substring($start))
    return ,$args.ToArray()
}

function Ensure-Import([string]$text) {
    if ($text -match 'import az\.nuran\.obsidianwars\.util\.ColorUtils;') { return $text }
    if ($text -notmatch 'ColorUtils') { return $text }
    if ($text -match '(?s)(package [^;]+;\s*)(import )') {
        return $text -replace '(package [^;]+;\s*)', "`$1`r`nimport az.nuran.obsidianwars.util.ColorUtils;`r`n"
    }
    return $text
}

Get-ChildItem (Join-Path $root 'src') -Recurse -Include *.java,*.yml | ForEach-Object {
    Convert-ColorBytes $_.FullName
}

$javaFiles = Get-ChildItem (Join-Path $root 'src') -Recurse -Filter *.java
foreach ($file in $javaFiles) {
    if ($file.Name -eq 'ColorUtils.java') { continue }
    $text = [System.IO.File]::ReadAllText($file.FullName)

    $text = $text -replace 'ChatColor\.translateAlternateColorCodes\(''&'',\s*', 'ColorUtils.translate('
    $text = $text -replace '\.replace\("&", "§"\)', ''
    $text = $text -replace '\.replace\("&", ""\)', ''

    # After removing .replace("&", "§"), expressions like ColorUtils.translate is still needed:
    # "noPermMessage" leftover as sendMessage(noPermMessage) - wrap below.

    $text = Wrap-CallArgs $text 'sendMessage' { param($a) Wrap-Translate $a }
    $text = Wrap-CallArgs $text 'sendActionBar' { param($a) Wrap-Translate $a }
    $text = Wrap-CallArgs $text 'setDisplayName' { param($a) Wrap-Translate $a }
    $text = Wrap-CallArgs $text 'setLore' {
        param($a)
        $trim = $a.Trim()
        if ($trim.StartsWith('ColorUtils.translate(')) { return $a }
        if ($trim.StartsWith('java.util.Arrays.asList') -or $trim.StartsWith('Arrays.asList') -or $trim.StartsWith('Collections.singletonList') -or $trim.StartsWith('List.of')) {
            return 'ColorUtils.translate(' + $a + ')'
        }
        return Wrap-Translate $a
    }
    $text = Wrap-CallArgs $text 'sendTitle' {
        param($a)
        $parts = @(Split-TopLevelArgs $a)
        if ($parts.Count -ge 2) {
            $parts[0] = Wrap-Translate $parts[0]
            $parts[1] = Wrap-Translate $parts[1]
            return ($parts -join ',')
        }
        return $a
    }
    $text = Wrap-CallArgs $text 'createInventory' {
        param($a)
        $parts = @(Split-TopLevelArgs $a)
        if ($parts.Count -ge 3) {
            $parts[2] = Wrap-Translate $parts[2]
            return ($parts -join ',')
        }
        return $a
    }
    $text = Wrap-CallArgs $text 'broadcastMessage' { param($a) Wrap-Translate $a }

    $text = Ensure-Import $text
    [System.IO.File]::WriteAllText($file.FullName, $text)
}

Write-Output 'Done applying color conversions.'
