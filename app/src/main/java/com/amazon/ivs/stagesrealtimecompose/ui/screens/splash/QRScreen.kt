package com.amazon.ivs.stagesrealtimecompose.ui.screens.splash

import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.amazon.ivs.stagesrealtimecompose.R
import com.amazon.ivs.stagesrealtimecompose.core.handlers.Destination
import com.amazon.ivs.stagesrealtimecompose.core.handlers.NavigationHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.UserHandler
import com.amazon.ivs.stagesrealtimecompose.ui.components.PreviewSurface
import com.amazon.ivs.stagesrealtimecompose.ui.theme.BlackPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.GreenPrimary
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import timber.log.Timber

@OptIn(ExperimentalGetImage::class)
@Composable
fun QRScreen() {
    val context = LocalContext.current
    val cameraExecutor by remember { mutableStateOf(ContextCompat.getMainExecutor(context)) }
    var isDisposed by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val session by UserHandler.session.collectAsStateWithLifecycle()

    LaunchedEffect(key1 = Unit) {
        UserHandler.codeValidated.collect {
            Timber.d("QR scanned, session set: $session")
            cameraExecutor.asCoroutineDispatcher().cancel()
            ProcessCameraProvider.getInstance(context).cancel(true)
        }
    }

    DisposableEffect(key1 = Unit) {
        onDispose {
            Timber.d("QR screen disposed")
            UserHandler.clearLastCode()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (isDisposed) return@Box

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val previewView = PreviewView(context)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val cameraPreview = Preview.Builder().setTargetRotation(previewView.display.rotation).build()
                        val barcodeScanner = BarcodeScanning.getClient(
                            BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                .build()
                        )
                        val analysis = ImageAnalysis.Builder()
                            .setTargetRotation(previewView.display.rotation)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        cameraPreview.surfaceProvider = previewView.surfaceProvider
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            processImageProxy(
                                barcodeScanner = barcodeScanner,
                                imageProxy = imageProxy
                            )
                        }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            cameraPreview,
                            analysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }, cameraExecutor)
                previewView
            },
        )
        CameraOverlay()
    }
}

@Composable
private fun CameraOverlay() {
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = 0.99f
            }
            .drawBehind {
                val paddingPx = with(density) { 32.dp.toPx() }
                val cornerLengthPx = with(density) { 32.dp.toPx() }
                val strokeWidthPx = with(density) { 3.dp.toPx() }
                val cornerColor = GreenPrimary

                val cutoutWidth = size.width - paddingPx * 2
                val cutoutHeight = size.height / 3

                val left = paddingPx
                val top = (size.height - cutoutHeight) / 2
                val right = size.width - paddingPx
                val bottom = top + cutoutHeight

                drawRect(color = BlackPrimary.copy(alpha = 0.4f))
                drawRect(
                    color = Color.Transparent,
                    topLeft = Offset(left, top),
                    size = Size(cutoutWidth, cutoutHeight),
                    blendMode = BlendMode.DstIn
                )
                drawLine(
                    color = cornerColor,
                    start = Offset(left - strokeWidthPx / 2, top),
                    end = Offset(left + cornerLengthPx, top),
                    strokeWidth = strokeWidthPx
                )
                drawLine(
                    color = cornerColor,
                    start = Offset(left, top),
                    end = Offset(left, top + cornerLengthPx),
                    strokeWidth = strokeWidthPx
                )
                drawLine(
                    color = cornerColor,
                    start = Offset(right + strokeWidthPx / 2, top),
                    end = Offset(right - cornerLengthPx, top),
                    strokeWidth = strokeWidthPx
                )
                drawLine(
                    color = cornerColor,
                    start = Offset(right, top),
                    end = Offset(right, top + cornerLengthPx),
                    strokeWidth = strokeWidthPx
                )
                drawLine(
                    color = cornerColor,
                    start = Offset(left - strokeWidthPx / 2, bottom),
                    end = Offset(left + cornerLengthPx, bottom),
                    strokeWidth = strokeWidthPx
                )
                drawLine(
                    color = cornerColor,
                    start = Offset(left, bottom),
                    end = Offset(left, bottom - cornerLengthPx),
                    strokeWidth = strokeWidthPx
                )
                drawLine(
                    color = cornerColor,
                    start = Offset(right + strokeWidthPx / 2, bottom),
                    end = Offset(right - cornerLengthPx, bottom),
                    strokeWidth = strokeWidthPx
                )
                drawLine(
                    color = cornerColor,
                    start = Offset(right, bottom),
                    end = Offset(right, bottom - cornerLengthPx),
                    strokeWidth = strokeWidthPx
                )
            }
    )
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(barcodeScanner: BarcodeScanner, imageProxy: ImageProxy) {
    imageProxy.image?.let { image ->
        val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (NavigationHandler.destination.value != Destination.QR) return@addOnSuccessListener
                if (barcodes.isNotEmpty()) {
                    val code = barcodes.first().rawValue ?: ""
                    UserHandler.enterCode(code = code)
                }
            }
            .addOnFailureListener { error ->
                Timber.d(error, "Failed to process QR code")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun CameraOverlayPreview() {
    PreviewSurface {
        Box {
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = R.drawable.bg_audio_stage,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
            CameraOverlay()
        }
    }
}
