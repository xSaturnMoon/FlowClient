$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$outDir = "C:\Users\tobia\Desktop\progetti\FlowClient\FlowClientBrandingMod\src\main\resources\assets\flowclient\textures\gui"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$size = 32
$bmp = New-Object System.Drawing.Bitmap $size, $size, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.Clear([System.Drawing.Color]::Transparent)
$pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::White), 2.2
$g.DrawEllipse($pen, 11, 6, 10, 10)
$path = New-Object System.Drawing.Drawing2D.GraphicsPath
$path.AddArc(7, 17, 18, 14, 180, 180)
$g.DrawPath($pen, $path)
$bmp.Save((Join-Path $outDir "icon_singleplayer.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$pen.Dispose(); $path.Dispose(); $g.Dispose(); $bmp.Dispose()
Write-Host "Icon saved"
