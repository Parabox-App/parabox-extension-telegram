package com.ojhdtapp.parabox.extension.telegram.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PauseCircleOutline
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ojhdtapp.parabox.extension.telegram.MainActivity
import com.ojhdtapp.parabox.extension.telegram.R
import com.ojhdtapp.parabox.extension.telegram.domain.util.ServiceStatus
import com.ojhdtapp.parabox.extension.telegram.ui.util.NormalPreference
import com.ojhdtapp.parabox.extension.telegram.ui.util.PreferencesCategory
import com.ojhdtapp.parabox.extension.telegram.ui.util.SwitchPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {

    val context = LocalContext.current

    val isMainAppInstalled = viewModel.isMainAppInstalled.collectAsState().value
    val serviceStatus = viewModel.serviceStatusStateFlow.collectAsState().value

    // snackBar
    val snackBarHostState = remember { SnackbarHostState() }
    LaunchedEffect(true) {
        viewModel.uiEventFlow.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackBarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    // TopBar Scroll Behaviour
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var menuExpanded by remember {
        mutableStateOf(false)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            LargeTopAppBar(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                title = { Text(text = stringResource(id = R.string.app_name)) },
                navigationIcon = {
                    if (isMainAppInstalled) {
                        IconButton(onClick = { (context as MainActivity).launchMainApp() }) {
                            Icon(
                                imageVector = Icons.Outlined.Home,
                                contentDescription = "back"
                            )
                        }
                    }
                },
                actions = {
                    Box() {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = "menu")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(id = R.string.force_stop_service)) },
                                onClick = {
                                    (context as MainActivity).forceStopParaboxService { }
                                    menuExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "stop service"
                                    )
                                })
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                MainSwitch(
                    textOff = stringResource(id = R.string.main_switch_off),
                    textOn = stringResource(id = R.string.main_switch_on),
                    checked = serviceStatus !is ServiceStatus.Stop,
                    onCheckedChange = {

                        if (it) {
                            (context as MainActivity).startParaboxService {

                            }
                        } else {
                            (context as MainActivity).stopParaboxService {

                            }
                        }
                    },
                    enabled = serviceStatus is ServiceStatus.Stop || serviceStatus is ServiceStatus.Running
                )
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                StatusIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    status = serviceStatus
                )
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                PreferencesCategory(text = stringResource(id = R.string.action_category))
            }
            item {
                SwitchPreference(
                    title = stringResource(id = R.string.auto_login_title),
                    subtitle = stringResource(id = R.string.auto_login_subtitle),
                    checked = viewModel.autoLoginSwitchFlow.collectAsState(initial = false).value,
                    onCheckedChange = viewModel::setAutoLoginSwitch
                )
            }
            item {
                SwitchPreference(
                    title = stringResource(id = R.string.foreground_service_title),
                    subtitle = stringResource(id = R.string.foreground_service_subtitle),
                    checked = viewModel.foregroundServiceSwitchFlow.collectAsState(initial = true).value,
                    onCheckedChange = viewModel::setForegroundServiceSwitch
                )
            }
            item {
                PreferencesCategory(text = stringResource(id = R.string.test_category))
            }
            item {
                NormalPreference(title = stringResource(id = R.string.test_send_message)) {
                    (context as MainActivity).receiveTestMessage()
                }
            }
        }
    }
}

@Composable
fun MainSwitch(
    modifier: Modifier = Modifier,
    textOff: String,
    textOn: String,
    checked: Boolean,
    onCheckedChange: (value: Boolean) -> Unit,
    enabled: Boolean
) {
    val switchColor by animateColorAsState(targetValue = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(32.dp))
            .clickable {
                if (enabled) onCheckedChange(!checked)
            },
        color = switchColor,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp, 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (checked) textOn else textOff,
                style = MaterialTheme.typography.titleLarge,
                color = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    }
}

@Composable
fun StatusIndicator(modifier: Modifier = Modifier, status: ServiceStatus) {
    AnimatedVisibility(
        visible = status !is ServiceStatus.Stop,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        val backgroundColor by animateColorAsState(
            targetValue = when (status) {
                is ServiceStatus.Error -> MaterialTheme.colorScheme.errorContainer
                is ServiceStatus.Loading -> MaterialTheme.colorScheme.primary
                is ServiceStatus.Running -> MaterialTheme.colorScheme.primary
                is ServiceStatus.Stop -> MaterialTheme.colorScheme.primary
                is ServiceStatus.Pause -> MaterialTheme.colorScheme.primary
            }
        )
        val textColor by animateColorAsState(
            targetValue = when (status) {
                is ServiceStatus.Error -> MaterialTheme.colorScheme.onErrorContainer
                is ServiceStatus.Loading -> MaterialTheme.colorScheme.onPrimary
                is ServiceStatus.Running -> MaterialTheme.colorScheme.onPrimary
                is ServiceStatus.Stop -> MaterialTheme.colorScheme.onPrimary
                is ServiceStatus.Pause -> MaterialTheme.colorScheme.onPrimary
            }
        )
        Row(modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(backgroundColor)
            .clickable { }
            .padding(24.dp, 24.dp),
            verticalAlignment = Alignment.CenterVertically) {
            when (status) {
                is ServiceStatus.Error -> Icon(
                    modifier = Modifier.padding(PaddingValues(end = 24.dp)),
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = "error",
                    tint = textColor
                )

                is ServiceStatus.Loading -> CircularProgressIndicator(
                    modifier = Modifier
                        .padding(PaddingValues(end = 24.dp))
                        .size(24.dp),
                    color = textColor,
                    strokeWidth = 3.dp
                )

                is ServiceStatus.Running -> Icon(
                    modifier = Modifier.padding(PaddingValues(end = 24.dp)),
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "running",
                    tint = textColor
                )

                is ServiceStatus.Stop -> Icon(
                    modifier = Modifier.padding(PaddingValues(end = 24.dp)),
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "stop",
                    tint = textColor
                )

                is ServiceStatus.Pause -> Icon(
                    modifier = Modifier.padding(PaddingValues(end = 24.dp)),
                    imageVector = Icons.Outlined.PauseCircleOutline,
                    contentDescription = "pause",
                    tint = textColor
                )
            }
            Column() {
                Text(
                    text = when (status) {
                        is ServiceStatus.Error -> stringResource(id = R.string.status_error)
                        is ServiceStatus.Loading -> stringResource(id = R.string.status_loading)
                        is ServiceStatus.Running -> stringResource(id = R.string.status_running)
                        is ServiceStatus.Stop -> ""
                        is ServiceStatus.Pause -> stringResource(id = R.string.status_pause)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = status.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }
    }
}