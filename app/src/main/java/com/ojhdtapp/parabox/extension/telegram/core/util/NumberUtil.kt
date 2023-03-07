package com.ojhdtapp.parabox.extension.telegram.core.util

fun String.inPhoneNumber(): Boolean{
    return this.matches(Regex("^[0-9]{11}\$"))
}