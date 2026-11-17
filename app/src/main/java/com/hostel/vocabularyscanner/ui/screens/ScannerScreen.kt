package com.hostel.vocabularyscanner.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.hostel.vocabularyscanner.data.repository.WordRepository
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScannerScreen(
    userId: String,
    wordRepository: WordRepository,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    var displayBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessingText by remember { mutableStateOf(false) }
    var extractedWords by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedWordForDialog by remember { mutableStateOf<String?>(null) }
    var scanErrorMessage by remember { mutableStateOf<String?>(null) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Helper to process text from an InputImage using ML Kit
    val processInputImage: (InputImage) -> Unit = { inputImage ->
        isProcessingText = true
        scanErrorMessage = null
        extractedWords = emptyList()

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val wordsSet = mutableSetOf<String>()
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        val rawWords = line.text.split("\\s+".toRegex())
                        for (rawWord in rawWords) {
                            val cleaned = rawWord.lowercase()
                                .replace("[^a-zA-Z]".toRegex(), "")
                                .trim()

                            if (cleaned.length >= 3) {
                                wordsSet.add(cleaned)
                            }
                        }
                    }
                }
                extractedWords = wordsSet.sorted()
                isProcessingText = false
                if (extractedWords.isEmpty()) {
                    scanErrorMessage = "No recognizable printed text found in this image."
                }
            }
            .addOnFailureListener { e ->
                isProcessingText = false
                scanErrorMessage = e.localizedMessage ?: "Failed to read text from image."
            }
    }

    // Secondary Camera launcher (Preview bitmap fallback)
    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            displayBitmap = bitmap
            processInputImage(InputImage.fromBitmap(bitmap, 0))
        } else if (displayBitmap == null) {
            scanErrorMessage = "No image was captured from camera."
        }
    }

    // Primary Camera launcher for capturing full resolution image via Uri
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            val uri = tempPhotoUri!!
            try {
                val bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
                if (bitmap != null) {
                    displayBitmap = bitmap
                }
                processInputImage(InputImage.fromFilePath(context, uri))
            } catch (e: Exception) {
                scanErrorMessage = e.localizedMessage ?: "Failed to process photo."
            }
        } else {
            // Fallback to preview launcher if file Uri capture wasn't completed
            try {
                cameraPreviewLauncher.launch(null)
            } catch (_: Exception) {
            }
        }
    }

    fun launchCameraInternal() {
        scanErrorMessage = null
        try {
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: context.cacheDir
            val tempFile = File.createTempFile(
                "scanned_img_${System.currentTimeMillis()}",
                ".jpg",
                storageDir
            )
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
        } catch (_: Exception) {
            // Fallback to preview capture if Uri/File creation or intent launch fails
            try {
                cameraPreviewLauncher.launch(null)
            } catch (e2: Exception) {
                scanErrorMessage = "Failed to launch camera: ${e2.localizedMessage}"
            }
        }
    }

    // Camera permission request launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCameraInternal()
        } else {
            scanErrorMessage = "Camera permission is required to take photos."
        }
    }

    // Gallery image picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
                if (bitmap != null) {
                    displayBitmap = bitmap
                }
                processInputImage(InputImage.fromFilePath(context, uri))
            } catch (e: Exception) {
                scanErrorMessage = e.localizedMessage ?: "Failed to load selected image."
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan & Discover Words") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Two buttons: Camera & Gallery
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            launchCameraInternal()
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Take Photo")
                }

                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("From Gallery")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preview image
            displayBitmap?.let { bitmap ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Captured Image",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (isProcessingText) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text("Recognizing printed text...")
            }

            scanErrorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (extractedWords.isNotEmpty()) {
                Text(
                    text = "Tap any word to view definition & pronunciation:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    extractedWords.forEach { word ->
                        SuggestionChip(
                            onClick = {
                                selectedWordForDialog = word
                            },
                            label = {
                                Text(word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
                            }
                        )
                    }
                }
            }
        }
    }

    // Word Details Dialog
    selectedWordForDialog?.let { word ->
        WordDetailsDialog(
            word = word,
            userId = userId,
            wordRepository = wordRepository,
            onDismiss = {
                selectedWordForDialog = null
            }
        )
    }
}
