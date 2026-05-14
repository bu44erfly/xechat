import Foundation
import AppKit
import CoreGraphics

let inputPath = "/Users/uc/Desktop/test/xechat/图片10.png"
let outputPath = "/Users/uc/Desktop/test/xechat/图片10_no_yellow_green_blue.png"

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

        // 黄色: R/G高，B明显低
        let isYellowStrong = (r > 130 && g > 130 && b < 170) && (abs(r - g) < 90)
        // 浅黄色（如 #f5f1c7）也视作黄色背景
        let isYellowLight = (r > 180 && g > 170 && b < min(r, g) - 18)
        let isYellow = isYellowStrong || isYellowLight
        // 绿色: G明显高于R/B
        let isGreen = (g > r + 20) && (g > b + 15) && (g > 80)
        // 蓝色: B明显高于R/G
        let isBlue = (b > r + 12) && (b > g + 8) && (b > 80)

        if isYellow || isGreen || isBlue {
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
