package com.androidoctor.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidoctor.data.local.ConfirmStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfirmViewModel @Inject constructor(private val store: ConfirmStore) : ViewModel() {
    fun request(onSkipKnown: () -> Unit, onAsk: () -> Unit) {
        viewModelScope.launch { if (store.skip()) onSkipKnown() else onAsk() }
    }

    fun rememberChoice(skip: Boolean) {
        viewModelScope.launch { store.setSkip(skip) }
    }
}

/**
 * Returns a function that runs an action after the user confirms a system-wide change.
 * Skipped when the user ticked "Don't ask again". [message] is shown in the dialog.
 */
@Composable
fun rememberConfirmChange(vm: ConfirmViewModel = hiltViewModel()): (message: String, action: () -> Unit) -> Unit {
    var pending by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var dontAsk by remember { mutableStateOf(false) }
    pending?.let { (message, action) ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Change system settings?") },
            text = {
                Column {
                    Text(message)
                    Spacer(Modifier.width(8.dp))
                    Row(
                        Modifier.clickable { dontAsk = !dontAsk },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = dontAsk, onCheckedChange = { dontAsk = it })
                        Text("Don't ask again")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (dontAsk) vm.rememberChoice(true)
                    pending = null
                    action()
                }) { Text("Continue") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel") } },
        )
    }
    return { message, action -> vm.request(onSkipKnown = action, onAsk = { dontAsk = false; pending = message to action }) }
}
