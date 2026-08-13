package com.dariusepure.caractivitylog.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLayeredComponent
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.component.shadow
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.shader.toDynamicShader
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.common.Dimensions
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun rememberMarker(
    labelPosition: DefaultCartesianMarker.LabelPosition = DefaultCartesianMarker.LabelPosition.Top,
    showIndicator: Boolean = true,
): CartesianMarker {
    val labelBackground = rememberShapeComponent(
        fill = fill(MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = CorneredShape.Pill,
        shadow = shadow(radius = 4.dp, dy = 2.dp)
    )
    val label = rememberTextComponent(
        color = MaterialTheme.colorScheme.onSurface,
        background = labelBackground,
        padding = Dimensions(horizontalDp = 8f, verticalDp = 4f),
        typeface = android.graphics.Typeface.MONOSPACE,
    )
    val indicatorFront = rememberShapeComponent(fill(MaterialTheme.colorScheme.primary), CorneredShape.Pill)
    val indicatorCenter = rememberShapeComponent(fill(MaterialTheme.colorScheme.surface), CorneredShape.Pill)
    val indicatorRear = rememberShapeComponent(fill(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)), CorneredShape.Pill)
    val indicator = rememberLayeredComponent(
        rear = indicatorRear,
        front = rememberLayeredComponent(
            rear = indicatorCenter,
            front = indicatorFront,
            padding = Dimensions(allDp = 2f),
        ),
        padding = Dimensions(allDp = 3f),
    )
    val guideline = rememberLineComponent(fill(MaterialTheme.colorScheme.outlineVariant), 2.dp)
    return rememberDefaultCartesianMarker(
        label = label,
        labelPosition = labelPosition,
        indicator = if (showIndicator) { { _ -> indicator } } else null,
        guideline = guideline,
    )
}

@Composable
fun rememberAxisLabelComponent() = rememberTextComponent(
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    textSize = 10.sp,
)

@Composable
fun rememberAxisTitleComponent() = rememberTextComponent(
    color = MaterialTheme.colorScheme.onSurface,
    textSize = 12.sp,
    typeface = android.graphics.Typeface.DEFAULT_BOLD,
)

@Composable
fun ConsumptionLineChart(
    data: List<Pair<Date, Double>>,
    unit: String,
    modifier: Modifier = Modifier
) {
    if (data.size < 2) return

    val modelProducer = remember { CartesianChartModelProducer() }
    val sortedData = remember(data) { data.sortedBy { it.first } }
    
    LaunchedEffect(sortedData) {
        modelProducer.runTransaction {
            lineSeries {
                series(sortedData.map { it.second })
            }
        }
    }

    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val marker = rememberMarker()
    val primaryColor = MaterialTheme.colorScheme.primary
    val axisLabel = rememberAxisLabelComponent()
    val axisTitle = rememberAxisTitleComponent()

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.rememberLine(
                        fill = LineCartesianLayer.LineFill.single(fill(primaryColor)),
                        areaFill = LineCartesianLayer.AreaFill.single(
                            fill(Brush.verticalGradient(listOf(primaryColor.copy(alpha = 0.3f), Color.Transparent)).toDynamicShader())
                        )
                    )
                )
            ),
            startAxis = VerticalAxis.rememberStart(
                label = axisLabel,
                titleComponent = axisTitle,
                title = unit,
                guideline = rememberLineComponent(
                    fill = fill(MaterialTheme.colorScheme.outlineVariant),
                    thickness = 0.5.dp,
                )
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                label = axisLabel,
                titleComponent = axisTitle,
                title = "Date",
                guideline = rememberLineComponent(
                    fill = fill(MaterialTheme.colorScheme.outlineVariant),
                    thickness = 0.5.dp,
                ),
                valueFormatter = { _, value, _ ->
                    val index = value.toInt()
                    if (index in sortedData.indices) {
                        dateFormatter.format(sortedData[index].first)
                    } else ""
                }
            ),
            marker = marker
        ),
        modelProducer = modelProducer,
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(8.dp)
    )
}

@Composable
fun MileageLineChart(
    data: List<Pair<Date, Double>>,
    unit: String,
    modifier: Modifier = Modifier
) {
    if (data.size < 2) return

    val modelProducer = remember { CartesianChartModelProducer() }
    val sortedData = remember(data) { data.sortedBy { it.first } }
    
    LaunchedEffect(sortedData) {
        modelProducer.runTransaction {
            lineSeries {
                series(sortedData.map { it.second })
            }
        }
    }

    val marker = rememberMarker()
    val primaryColor = MaterialTheme.colorScheme.primary
    val axisLabel = rememberAxisLabelComponent()
    val axisTitle = rememberAxisTitleComponent()

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.rememberLine(
                        fill = LineCartesianLayer.LineFill.single(fill(primaryColor)),
                        areaFill = LineCartesianLayer.AreaFill.single(
                            fill(Brush.verticalGradient(listOf(primaryColor.copy(alpha = 0.3f), Color.Transparent)).toDynamicShader())
                        )
                    )
                )
            ),
            startAxis = VerticalAxis.rememberStart(
                label = axisLabel,
                titleComponent = axisTitle,
                title = unit,
                guideline = rememberLineComponent(
                    fill = fill(MaterialTheme.colorScheme.outlineVariant),
                    thickness = 0.5.dp,
                )
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                label = axisLabel,
                titleComponent = axisTitle,
                title = "Date",
                guideline = rememberLineComponent(
                    fill = fill(MaterialTheme.colorScheme.outlineVariant),
                    thickness = 0.5.dp,
                ),
                valueFormatter = { _, value, _ ->
                    val index = value.toInt()
                    if (index in sortedData.indices) {
                        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(sortedData[index].first)
                    } else ""
                }
            ),
            marker = marker
        ),
        modelProducer = modelProducer,
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(vertical = 16.dp)
    )
}

