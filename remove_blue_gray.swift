import Foundation
import AppKit
import CoreGraphics

let inputPath = "/Users/uc/Desktop/test/xechat/upload_file_er_diagram.png~tplv-a9rns2rl98-image-face-cut_0_0_959_723_959_723.jpeg"
let outputPath = "/Users/uc/Desktop/test/xechat/upload_file_er_diagram_no_blue_gray.png"

guard let image = NSImage(contentsOfFile: inputPath) else {
    fatalError("Failed to load input image")
}

var rect = NSRect(origin: .zero, size: image.size)
guard let cgImage = image.cgImage(forProposedRect: &rect, context: nil, hints: nil) else {
    fatalError("Failed to get CGImage")
}

let width = cgImage.width
let height = cgImage.height
let bytesPerPixel = 4
let bytesPerRow = width * bytesPerPixel

var data = [UInt8](repeating: 0, count: height * bytesPerRow)
let colorSpace = CGColorSpaceCreateDeviceRGB()

guard let context = CGContext(
    data: &data,
    width: width,
    height: height,
    bitsPerComponent: 8,
    bytesPerRow: bytesPerRow,
    space: colorSpace,
    bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
) else {
    fatalError("Failed to create bitmap context")
}

context.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))

for y in 0..<height {
    for x in 0..<width {
        let idx = y * bytesPerRow + x * 4
        let r = Int(data[idx])
        let g = Int(data[idx + 1])
        let b = Int(data[idx + 2])

        // 更宽松的蓝色判断，覆盖抗锯齿边缘残留
        let isBlue = (b > r + 6) && (b > g + 6) && (b > 90)

        let maxRGB = max(r, max(g, b))
        let minRGB = min(r, min(g, b))
        let isGray = (maxRGB - minRGB <= 16) && (r > 110 && g > 110 && b > 110)

        if isBlue || isGray {
            data[idx] = 255
            data[idx + 1] = 255
            data[idx + 2] = 255
            data[idx + 3] = 255
        }
    }
}

guard let outputCG = context.makeImage() else {
    fatalError("Failed to create output CGImage")
}

let rep = NSBitmapImageRep(cgImage: outputCG)
guard let pngData = rep.representation(using: .png, properties: [:]) else {
    fatalError("Failed to encode PNG")
}

try pngData.write(to: URL(fileURLWithPath: outputPath))
print(outputPath)
