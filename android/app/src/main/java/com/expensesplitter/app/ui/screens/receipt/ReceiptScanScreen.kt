package com.expensesplitter.app.ui.screens.receipt

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expensesplitter.app.data.repository.PendingReceiptDraftHolder
import com.expensesplitter.app.data.repository.ReceiptRepository
import com.expensesplitter.app.ui.components.MoneyLoadingIndicator
import com.expensesplitter.app.ui.theme.Spacing
import java.io.File

@Composable
fun ReceiptScanScreen(
    receiptRepository: ReceiptRepository,
    pendingReceiptDraftHolder: PendingReceiptDraftHolder,
    onScanned: () -> Unit,
) {
    val viewModel: ReceiptScanViewModel = viewModel(
        factory = viewModelFactory { initializer { ReceiptScanViewModel(receiptRepository, pendingReceiptDraftHolder) } },
    )
    val state = viewModel.state
    val context = LocalContext.current

    val cameraFile = remember { File(context.cacheDir, "receipt_capture.jpg") }
    val cameraUri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cameraFile)
    }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) viewModel.scan(cameraFile, "image/jpeg", onScanned)
    }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val resolver = context.contentResolver
        var displayName = "receipt.jpg"
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) displayName = cursor.getString(nameIndex)
        }
        val mimeType = resolver.getType(uri) ?: "image/jpeg"
        val tempFile = File(context.cacheDir, displayName)
        resolver.openInputStream(uri)?.use { input -> tempFile.outputStream().use { output -> input.copyTo(output) } }
        viewModel.scan(tempFile, mimeType, onScanned)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.Receipt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Take a photo or pick one from your gallery — we'll try to read the date, merchant, and total, " +
                "and you'll confirm everything before it's saved.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.isScanning) {
            MoneyLoadingIndicator()
            Text("Reading receipt…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Button(onClick = { takePictureLauncher.launch(cameraUri) }, modifier = Modifier.fillMaxWidth()) { Text("Take Photo") }
            OutlinedButton(
                onClick = { pickImageLauncher.launch(arrayOf("image/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Choose from Gallery") }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
