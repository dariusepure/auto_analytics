package com.dariusepure.caractivitylog.ui.cars

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.data.cars.CarRepository
import com.dariusepure.caractivitylog.domain.CarPhoto
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CarGalleryViewModel @Inject constructor(
    private val carRepository: CarRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _photos = MutableStateFlow<List<CarPhoto>>(emptyList())
    val photos: StateFlow<List<CarPhoto>> = _photos.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    fun loadPhotos(carId: String) {
        viewModelScope.launch {
            carRepository.getCarPhotos(carId).collect {
                _photos.value = it
            }
        }
    }

    fun addPhoto(carId: String, uri: Uri) {
        viewModelScope.launch {
            _isUploading.value = true
            try {
                val fileName = "${UUID.randomUUID()}.jpg"
                val destFile = File(context.filesDir, "car_photos/$fileName")
                destFile.parentFile?.mkdirs()

                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                if (destFile.exists()) {
                    carRepository.addCarPhoto(carId, CarPhoto(carId = carId, fileName = fileName))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun deletePhoto(carId: String, photo: CarPhoto) {
        viewModelScope.launch {
            try {
                val file = File(context.filesDir, "car_photos/${photo.fileName}")
                if (file.exists()) {
                    file.delete()
                }
                carRepository.deleteCarPhoto(carId, photo.id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
