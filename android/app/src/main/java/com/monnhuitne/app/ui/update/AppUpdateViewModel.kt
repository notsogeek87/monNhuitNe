package com.monnhuitne.app.ui.update

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lielu.githubupdater.UpdateConfig
import com.lielu.githubupdater.UpdateInfo
import com.lielu.githubupdater.UpdateManager
import com.lielu.githubupdater.UpdateState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Vérifie automatiquement les mises à jour à l'ouverture de l'app et pilote la fenêtre qui les propose.
 * L'état brut est celui de [UpdateManager], partagé avec l'écran Réglages.
 */
class AppUpdateViewModel(
    private val updateManager: UpdateManager,
    val updatesEnabled: Boolean,
) : ViewModel() {
    val state = updateManager.state

    private val _dismissed = MutableStateFlow(false)

    /** `true` une fois « Plus tard » touché : la fenêtre ne revient qu'au prochain lancement. */
    val dismissed: StateFlow<Boolean> = _dismissed

    private val _userStarted = MutableStateFlow(false)

    /** `true` dès que l'utilisateur a touché « Installer » : seulement alors on lui montre une erreur. */
    val userStarted: StateFlow<Boolean> = _userStarted

    private val _checkedManually = MutableStateFlow(false)

    /** `true` après un appui sur « Rechercher une mise à jour » : l'écran Réglages affiche alors le résultat. */
    val checkedManually: StateFlow<Boolean> = _checkedManually

    /** `false` tant qu'Android n'a pas autorisé l'app à installer des applications (étape à expliquer). */
    fun canInstallPackages(): Boolean = updateManager.canInstallPackages()

    /**
     * Appelée à chaque passage de l'app au premier plan. `force = true` : sans cela la bibliothèque réutilise
     * sa réponse précédente (« à jour ») jusqu'à `checkIntervalHours`, et une release publiée entre-temps
     * n'est pas vue. Une requête GitHub par ouverture reste très en dessous du quota (60/h).
     */
    fun checkOnOpen() {
        if (!updatesEnabled) return
        // Ne pas écraser une fenêtre déjà affichée, un téléchargement ou une installation en cours.
        val s = state.value
        if (s !is UpdateState.Idle && s !is UpdateState.UpToDate && s !is UpdateState.Error) return
        // Les erreurs (hors ligne, quota GitHub…) sont publiées dans `state` ; ici on reste silencieux.
        viewModelScope.launch { runCatching { updateManager.checkForUpdate(force = true) } }
    }

    /** Bouton « Rechercher une mise à jour » des Réglages. */
    fun checkManually() {
        if (!updatesEnabled) return
        val s = state.value
        if (s is UpdateState.Checking || s is UpdateState.Downloading || s is UpdateState.Installing) return
        _checkedManually.value = true
        _dismissed.value = false
        _userStarted.value = false
        viewModelScope.launch { runCatching { updateManager.checkForUpdate(force = true) } }
    }

    fun onDismiss() {
        _dismissed.value = true
    }

    fun onInstall(update: UpdateInfo) {
        _userStarted.value = true
        viewModelScope.launch {
            runCatching {
                val apk = updateManager.downloadUpdate(update)
                install(apk)
            }
        }
    }

    /** Relance l'installation d'un APK déjà téléchargé, typiquement au retour des réglages Android. */
    fun onInstallDownloaded(apk: File) {
        _userStarted.value = true
        runCatching { install(apk) }
    }

    private fun install(apk: File) {
        if (updateManager.canInstallPackages()) {
            updateManager.installUpdate(apk)
        } else {
            updateManager.openInstallPermissionSettings()
        }
    }

    companion object {
        private const val SUFFIXED_APPLICATION_ID = ".staging"

        fun create(context: Context): AppUpdateViewModel {
            val manager =
                UpdateManager(
                    context,
                    UpdateConfig(
                        githubOwner = "notsogeek87",
                        githubRepository = "monNhuitNe",
                        apkAssetNamePattern = ".*\\.apk",
                    ),
                )
            // Un applicationId à suffixe (.staging) ne peut pas se mettre à jour depuis la release de production.
            return AppUpdateViewModel(manager, updatesEnabled = !context.packageName.endsWith(SUFFIXED_APPLICATION_ID))
        }
    }
}
