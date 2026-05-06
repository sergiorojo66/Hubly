package com.example.rankup.ui.authScreen.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HublyTextField(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    placeholder: String = "",
    icon: ImageVector? = null,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onPasswordToggleClick: () -> Unit = {},
    onValueChange: (String) -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isError) Color.Red else Color.Gray,
            modifier = Modifier.padding(start = 4.dp)
        )

        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            readOnly = readOnly,
            leadingIcon = icon?.let {
                { Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = if (isError) Color.Red else Color.Gray
                ) }
            },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = onPasswordToggleClick) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = if (isError) Color.Red else Color.Gray
                        )
                    }
                }
            } else null,

            shape = RoundedCornerShape(12.dp),
            isError = isError,
            placeholder = {
                Text(placeholder, color = Color.Gray.copy(alpha = 0.6f))
            },
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black, // Cambiado de DarkGray a Black para mejor lectura
                focusedContainerColor = Color(0xFFF3F4F6),
                unfocusedContainerColor = Color(0xFFF3F4F6),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                errorContainerColor = Color(0xFFFFEBEE)
            ),
            keyboardOptions = keyboardOptions,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else 10,
        )

        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
            )
        }
    }
}