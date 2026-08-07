package com.rmm.recetasraquel.ui.editor

import com.rmm.recetasraquel.domain.photos.StagedPhoto

sealed interface EditorPhotoState {
    data object None : EditorPhotoState
    data class Persisted(val relativePath: String) : EditorPhotoState
    data class Staged(val stagedPhoto: StagedPhoto) : EditorPhotoState
    data class Processing(val previous: EditorPhotoState?) : EditorPhotoState
    data class Removed(val previous: EditorPhotoState) : EditorPhotoState
    data class Error(val previous: EditorPhotoState?, val message: String) : EditorPhotoState
}
