Add-Type -AssemblyName System.Drawing
$logoPath = "C:\Salat Tracker\logo.png"
$resPath = "C:\Salat Tracker\app\app\src\main\res"

$sizes = @{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

$originalImage = [System.Drawing.Image]::FromFile($logoPath)

foreach ($folder in $sizes.Keys) {
    $size = $sizes[$folder]
    $folderPath = Join-Path $resPath $folder
    if (-not (Test-Path $folderPath)) { New-Item -ItemType Directory -Path $folderPath | Out-Null }
    
    $resizedImage = new-object System.Drawing.Bitmap($size, $size)
    $graphics = [System.Drawing.Graphics]::FromImage($resizedImage)
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.DrawImage($originalImage, 0, 0, $size, $size)
    $graphics.Dispose()
    
    $launcherPath = Join-Path $folderPath "ic_launcher.png"
    $launcherRoundPath = Join-Path $folderPath "ic_launcher_round.png"
    
    $resizedImage.Save($launcherPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $resizedImage.Save($launcherRoundPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $resizedImage.Dispose()
}

$originalImage.Dispose()

# Remove old files
Remove-Item -Path (Join-Path $resPath "mipmap-anydpi-v26") -Recurse -Force -ErrorAction SilentlyContinue
Get-ChildItem -Path "C:\Salat Tracker\app\app\src\main\res\mipmap-*" -Include *.webp -Recurse | Remove-Item -Force
