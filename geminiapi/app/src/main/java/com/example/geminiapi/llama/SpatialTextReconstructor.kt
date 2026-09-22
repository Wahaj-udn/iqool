package com.example.geminiapi.llama

import android.graphics.Rect
import com.google.mlkit.vision.text.Text
import kotlin.math.abs

object SpatialTextReconstructor {

    data class TextElementInfo(
        val text: String,
        val boundingBox: Rect,
        val centerX: Float,
        val centerY: Float,
        val height: Int
    )

    fun reconstruct(visionText: Text): String {
        val elements = mutableListOf<TextElementInfo>()

        // Gather all fine-grained text lines or elements from the blocks
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox ?: continue
                elements.add(
                    TextElementInfo(
                        text = line.text,
                        boundingBox = box,
                        centerX = box.exactCenterX(),
                        centerY = box.exactCenterY(),
                        height = box.height()
                    )
                )
            }
        }

        if (elements.isEmpty()) return ""

        // Sort elements primarily by vertical position to process top-to-bottom
        elements.sortBy { it.centerY }

        val rows = mutableListOf<MutableList<TextElementInfo>>()

        for (element in elements) {
            var matchedRow: MutableList<TextElementInfo>? = null
            
            // Check if this element fits into an existing horizontal row based on vertical overlap/proximity
            for (row in rows) {
                val rowSample = row.first()
                val verticalDelta = abs(element.centerY - rowSample.centerY)
                // Use a threshold relative to the text heights to group close components together
                val maxAllowedDelta = (rowSample.height.coerceAtLeast(element.height) * 0.65f)
                
                if (verticalDelta <= maxAllowedDelta) {
                    matchedRow = row
                    break
                }
            }

            if (matchedRow != null) {
                matchedRow.add(element)
            } else {
                rows.add(mutableListOf(element))
            }
        }

        val resultBuilder = StringBuilder()

        // Process each row: sort elements from left to right and format them nicely
        for (row in rows) {
            // Sort elements left-to-right using their bounding box X coordinate
            row.sortBy { it.boundingBox.left }

            if (row.isEmpty()) continue

            if (row.size == 1) {
                resultBuilder.append(row[0].text).append("\n")
            } else {
                // Multi-element row (e.g. NutrientName + Value / Percentage)
                // Format nicely with delimiters to preserve visual structural alignment relationship
                val rowString = row.joinToString(separator = " : ") { it.text.trim() }
                resultBuilder.append(rowString).append("\n")
            }
        }

        return resultBuilder.toString()
    }
}
