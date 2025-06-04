package com.amazon.ivs.stagesrealtime.ui.lobby

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.amazon.ivs.stagesrealtime.R
import com.amazon.ivs.stagesrealtime.common.extensions.collectLatestWithLifecycle
import com.amazon.ivs.stagesrealtime.common.extensions.fadeAlpha
import com.amazon.ivs.stagesrealtime.common.extensions.navController
import com.amazon.ivs.stagesrealtime.common.extensions.showErrorBar
import com.amazon.ivs.stagesrealtime.common.viewBinding
import com.amazon.ivs.stagesrealtime.databinding.FragmentScannerBinding
import com.amazon.ivs.stagesrealtime.ui.BackHandler
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class ScannerFragment : Fragment(R.layout.fragment_scanner), BackHandler {
    private val binding by viewBinding(FragmentScannerBinding::bind)
    private val viewModel by viewModels<LobbyViewModel>()

    private val cameraSelector by lazy {
        CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
    }
    private val cameraProviderFuture by lazy {
        ProcessCameraProvider.getInstance(requireContext())
    }

    private lateinit var processCameraProvider: ProcessCameraProvider
    private lateinit var cameraPreview: Preview
    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var cameraExecutor: ExecutorService
    private var processingBarcode = false
    private var showingErrorBar = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCollectors()
        setupListeners()
    }

    override fun handleBackPress() {
        navController.navigateUp()
    }

    private fun setupCollectors() {
        collectLatestWithLifecycle(viewModel.onCustomerCodeSet) { isSet ->
            Timber.d("Valid api key and code received: $isSet")
            processingBarcode = false
            if (isSet) {
                cameraExecutor.shutdown()
                handleBackPress()
            } else {
                if (!showingErrorBar) {
                    showingErrorBar = true
                    showErrorBar(R.string.error_customer_code) {
                        showingErrorBar = false
                    }
                }
            }
        }

        collectLatestWithLifecycle(viewModel.isLoading) { isLoading ->
            binding.loadingView.root.fadeAlpha(isLoading)
        }
    }

    private fun setupListeners() = with(binding) {
        cameraProviderFuture.addListener({
                processCameraProvider = cameraProviderFuture.get()
                cameraPreview = Preview.Builder().setTargetRotation(previewView.display.rotation).build()
                cameraPreview.surfaceProvider = previewView.surfaceProvider
                processCameraProvider.bindToLifecycle(this@ScannerFragment, cameraSelector, cameraPreview)

                bindInputAnalyser()
            },
            ContextCompat.getMainExecutor(requireContext())
        )

        backButton.setOnClickListener {
            handleBackPress()
        }
    }

    private fun bindInputAnalyser() {
        val barcodeScanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetRotation(binding.previewView.display.rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        cameraExecutor = Executors.newSingleThreadExecutor()
        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            processImageProxy(barcodeScanner, imageProxy)
        }

        processCameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(barcodeScanner: BarcodeScanner, imageProxy: ImageProxy) {
        imageProxy.image?.let { image ->
            val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)

            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty() && !processingBarcode) {
                        val barcode = barcodes.first().rawValue ?: ""
                        Timber.d("Scanned barcode: $barcode")
                        processingBarcode = true
                        viewModel.signIn(barcode)
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
}
