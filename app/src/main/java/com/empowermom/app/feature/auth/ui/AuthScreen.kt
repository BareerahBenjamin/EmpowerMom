package com.empowermom.app.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.empowermom.app.core.ui.theme.EmpowerMomColors
import com.empowermom.app.feature.auth.viewmodel.AuthMode
import com.empowermom.app.feature.auth.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isLogin = uiState.mode == AuthMode.Login

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        EmpowerMomColors.PeachPale
                    )
                )
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "EmpowerMom",
                style = MaterialTheme.typography.headlineLarge,
                color = EmpowerMomColors.TextDark,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "登录后同步留言、记录和个人资料",
                style = MaterialTheme.typography.bodyMedium,
                color = EmpowerMomColors.TextMid,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AuthModeSwitch(
                        selectedMode = uiState.mode,
                        onModeChange = viewModel::setMode
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("邮箱") },
                        placeholder = { Text("请输入邮箱地址") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = authTextFieldColors()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("密码") },
                        placeholder = { Text("至少 6 位密码") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = authTextFieldColors()
                    )

                    uiState.errorMessage?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = {
                            if (isLogin) {
                                viewModel.signIn(email, password, onLoginSuccess)
                            } else {
                                viewModel.signUp(email, password, onLoginSuccess)
                            }
                        },
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmpowerMomColors.Rose,
                            disabledContainerColor = EmpowerMomColors.PeachLight
                        )
                    ) {
                        Text(
                            text = when {
                                uiState.isLoading -> "请稍候..."
                                isLogin -> "登录"
                                else -> "注册"
                            },
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    TextButton(
                        onClick = {
                            viewModel.setMode(
                                if (isLogin) AuthMode.SignUp else AuthMode.Login
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isLogin) "还没有账号？去注册" else "已有账号？去登录",
                            color = EmpowerMomColors.Rose
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthModeSwitch(
    selectedMode: AuthMode,
    onModeChange: (AuthMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EmpowerMomColors.PeachPale, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AuthModeButton(
            text = "登录",
            selected = selectedMode == AuthMode.Login,
            onClick = { onModeChange(AuthMode.Login) },
            modifier = Modifier.weight(1f)
        )
        AuthModeButton(
            text = "注册",
            selected = selectedMode == AuthMode.SignUp,
            onClick = { onModeChange(AuthMode.SignUp) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AuthModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.surface else EmpowerMomColors.PeachPale
    val contentColor = if (selected) EmpowerMomColors.Rose else EmpowerMomColors.TextMid

    Button(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = EmpowerMomColors.Peach,
    unfocusedBorderColor = EmpowerMomColors.PeachLight,
    focusedLabelColor = EmpowerMomColors.Rose,
    cursorColor = EmpowerMomColors.Rose
)
