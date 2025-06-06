package com.example.sensor_magnometro

import android.content.Context
import android.hardware.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensor_magnometro.ui.theme.Sensor_magnometroTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Sensor_magnometroTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CompassScreen()
                }
            }
        }
    }
}

// Función de filtro de paso bajo para suavizar los datos del sensor
fun lowPass(input: FloatArray, output: FloatArray?): FloatArray {
    val alpha = 0.1f
    if (output == null) return input
    for (i in input.indices) {
        output[i] = output[i] + alpha * (input[i] - output[i])
    }
    return output
}

@Composable
fun CompassScreen() {
    val context = LocalContext.current
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val azimuth = remember { mutableStateOf(0f) }

    DisposableEffect(Unit) {
        var gravity = FloatArray(3)
        var geomagnetic = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                when (event?.sensor?.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        gravity = lowPass(event.values.clone(), gravity)
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        geomagnetic = lowPass(event.values.clone(), geomagnetic)
                    }
                }

                val R = FloatArray(9)
                val I = FloatArray(9)
                if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(R, orientation)
                    azimuth.value = Math.toDegrees(orientation[0].toDouble()).toFloat()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.compass),
                contentDescription = "Compass",
                modifier = Modifier
                    .size(250.dp)
                    .graphicsLayer { rotationZ = -azimuth.value }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Dirección: ${azimuth.value.toInt()}°", fontSize = 20.sp)
        }
    }
}
