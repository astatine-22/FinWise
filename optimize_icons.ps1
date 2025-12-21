
try {
    Add-Type -AssemblyName System.Drawing
} catch {
    Write-Error "System.Drawing not available."
    exit 1
}

$baseDir = Get-Location
$resDir = Join-Path $baseDir "app\src\main\res"
$sourceFile = Join-Path $resDir "drawable\finwise_logo.jpg"

Write-Host "Processing $sourceFile"

if (-not (Test-Path $sourceFile)) {
    Write-Error "File not found: $sourceFile"
    exit 1
}

$img = [System.Drawing.Bitmap]::FromFile($sourceFile)
Write-Host "Image Size: $($img.Width) x $($img.Height)"

# 1. Scan for content (non-white)
$minX = $img.Width; $maxX = 0; $minY = $img.Height; $maxY = 0
$threshold = 240
$found = $false

for ($y=0; $y -lt $img.Height; $y+=10) {
    for ($x=0; $x -lt $img.Width; $x+=10) {
        $c = $img.GetPixel($x, $y)
        if ($c.R -lt $threshold -or $c.G -lt $threshold -or $c.B -lt $threshold) {
            $found = $true
            if ($x -lt $minX) { $minX = $x }
            if ($x -gt $maxX) { $maxX = $x }
            if ($y -lt $minY) { $minY = $y }
            if ($y -gt $maxY) { $maxY = $y }
        }
    }
}

if ($found) {
    # Add 5% padding
    $pad = [int](($maxX - $minX) * 0.05)
    $minX = [math]::Max(0, $minX - $pad)
    $maxX = [math]::Min($img.Width, $maxX + $pad)
    $minY = [math]::Max(0, $minY - $pad)
    $maxY = [math]::Min($img.Height, $maxY + $pad)
    Write-Host "Found content bounds: $minX,$minY $maxX,$maxY"
} else {
    Write-Host "No content found (all white?). Falling back to Center 60%"
    $minX = [int]($img.Width * 0.20)
    $maxX = [int]($img.Width * 0.80)
    $minY = [int]($img.Height * 0.20)
    $maxY = [int]($img.Height * 0.80)
}

$w = $maxX - $minX
$h = $maxY - $minY

if ($w -le 0 -or $h -le 0) {
    Write-Error "Invalid crop dimensions: $w x $h"
    exit 1
}

$rect = New-Object System.Drawing.Rectangle $minX, $minY, $w, $h
try {
    $cropped = $img.Clone($rect, $img.PixelFormat)
} catch {
    Write-Error "Crop failed: $_"
    exit 1
}

# 2. Save Helper
function Save-Resized($bmp, $size, $path) {
    $new = New-Object System.Drawing.Bitmap $size, $size
    $g = [System.Drawing.Graphics]::FromImage($new)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.DrawImage($bmp, 0, 0, $size, $size)
    $dir = Split-Path $path
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    
    # Save using PNG encoder explicitly if needed, but simple Save works for file extension
    $new.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $new.Dispose()
}

# 3. Process Legacy
$densities = @{
    "mdpi" = 48
    "hdpi" = 72
    "xhdpi" = 96
    "xxhdpi" = 144
    "xxxhdpi" = 192
}

foreach ($key in $densities.Keys) {
    $size = $densities[$key]
    $p = Join-Path $resDir "mipmap-$key\ic_launcher.png"
    Write-Host "Saving $key ($size) to $p"
    Save-Resized $cropped $size $p
    
    $p2 = Join-Path $resDir "mipmap-$key\ic_launcher_round.png"
    Save-Resized $cropped $size $p2
}

# 4. Adaptive Foreground
# 432x432 canvas. Content 280x280 (65%).
$adaptSize = 432
$contentSize = 280
$offset = [int](($adaptSize - $contentSize)/2)

$adaptBmp = New-Object System.Drawing.Bitmap $adaptSize, $adaptSize
$g = [System.Drawing.Graphics]::FromImage($adaptBmp)
$g.Clear([System.Drawing.Color]::Transparent)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.DrawImage($cropped, $offset, $offset, $contentSize, $contentSize)

$adaptPath = Join-Path $resDir "drawable\ic_launcher_foreground_optimized.png"
Write-Host "Saving Adaptive Foreground to $adaptPath"
$adaptBmp.Save($adaptPath, [System.Drawing.Imaging.ImageFormat]::Png)

$g.Dispose()
$adaptBmp.Dispose()
$cropped.Dispose()
$img.Dispose()
