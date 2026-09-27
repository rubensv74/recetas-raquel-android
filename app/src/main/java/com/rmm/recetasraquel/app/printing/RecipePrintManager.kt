package com.rmm.recetasraquel.app.printing

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.util.Base64
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.rmm.recetasraquel.app.RecipePhotoPathMapper
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetySummary
import com.rmm.recetasraquel.domain.model.Recipe
import java.io.File

object RecipePrintManager {
    private val activeWebViews = mutableSetOf<WebView>()

    fun print(
        context: Context,
        recipe: Recipe,
        safetySummary: RecipeSafetySummary?,
        safetyMessage: String?,
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val coverDataUri = recipe.coverPhotoPath?.let { resolvePhotoDataUri(context, it) }
        val stepPhotoDataUris = recipe.steps
            .mapNotNull { step ->
                step.photoPath?.let { relativePath ->
                    resolvePhotoDataUri(context, relativePath)?.let { step.id to it }
                }
            }
            .toMap()
        val html = RecipePrintHtmlBuilder.build(
            recipe = recipe,
            safetySummary = safetySummary,
            safetyMessage = safetyMessage,
            coverPhotoDataUri = coverDataUri,
            stepPhotoDataUris = stepPhotoDataUris,
        )

        val webView = WebView(context).apply {
            settings.javaScriptEnabled = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.loadsImagesAutomatically = true
        }

        activeWebViews += webView

        var printStarted = false
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                if (printStarted) return
                printStarted = true

                val jobName = buildJobName(recipe.name)
                val delegate = view.createPrintDocumentAdapter(jobName)
                val adapter = RetainedPrintDocumentAdapter(delegate) {
                    activeWebViews -= view
                    view.destroy()
                }

                val attributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                    .build()

                printManager.print(jobName, adapter, attributes)
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError,
            ) {
                if (request.isForMainFrame && !printStarted) {
                    activeWebViews -= view
                    view.destroy()
                }
            }
        }

        webView.loadDataWithBaseURL(
            null,
            html,
            "text/html",
            "UTF-8",
            null,
        )
    }

    private fun resolvePhotoDataUri(context: Context, relativePath: String): String? {
        return runCatching {
            val file = RecipePhotoPathMapper.resolve(context.filesDir, relativePath)
                ?: File(context.filesDir, relativePath).takeIf { it.isFile }
                ?: return null
            val mimeType = when (file.extension.lowercase()) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                else -> "image/jpeg"
            }
            val encoded = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
            "data:$mimeType;base64,$encoded"
        }.getOrNull()
    }

    private fun buildJobName(recipeName: String): String {
        val cleanName = recipeName.trim().ifBlank { "Receta" }.take(70)
        return "Recetoria - $cleanName"
    }

    private class RetainedPrintDocumentAdapter(
        private val delegate: PrintDocumentAdapter,
        private val release: () -> Unit,
    ) : PrintDocumentAdapter() {
        override fun onStart() {
            delegate.onStart()
        }

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal,
            callback: LayoutResultCallback,
            extras: Bundle?,
        ) {
            delegate.onLayout(
                oldAttributes,
                newAttributes,
                cancellationSignal,
                callback,
                extras,
            )
        }

        override fun onWrite(
            pages: Array<PageRange>,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal,
            callback: WriteResultCallback,
        ) {
            delegate.onWrite(
                pages,
                destination,
                cancellationSignal,
                callback,
            )
        }

        override fun onFinish() {
            try {
                delegate.onFinish()
            } finally {
                release()
            }
        }
    }
}
