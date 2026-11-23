package com.androidoctor.ads

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Gives composables access to the singleton [ConsentManager]. */
@HiltViewModel
class ConsentViewModel @Inject constructor(val consent: ConsentManager) : ViewModel()
