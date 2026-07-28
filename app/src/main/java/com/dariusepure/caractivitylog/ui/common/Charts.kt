package com.dariusepure.caractivitylog.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(
                title = unit
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = { _, value, _ ->
                    val index = value.toInt()
                    if (index in sortedData.indices) {
                        dateFormatter.format(sortedData[index].first)
                    } else ""
                }
            )
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

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(
                title = unit
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = { _, value, _ ->
                    val index = value.toInt()
                    if (index in sortedData.indices) {
                        dateFormatter.format(sortedData[index].first)
                    } else ""
                }
            )
        ),
        modelProducer = modelProducer,
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(8.dp)
    )
}
