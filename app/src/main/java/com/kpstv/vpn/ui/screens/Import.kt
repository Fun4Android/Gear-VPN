package com.kpstv.vpn.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.statusBarsPadding
import com.kpstv.vpn.R
import com.kpstv.vpn.data.models.LocalConfiguration
import com.kpstv.vpn.extensions.clickableNoIndication
import com.kpstv.vpn.extensions.utils.AppUtils.asPassword
import com.kpstv.vpn.extensions.utils.AppUtils.getFileName
import com.kpstv.vpn.extensions.utils.AppUtils.launchUrlInApp
import com.kpstv.vpn.extensions.utils.VpnUtils
import com.kpstv.vpn.ui.components.*
import com.kpstv.vpn.ui.helpers.Settings
import com.kpstv.vpn.ui.theme.CommonPreviewTheme
import com.kpstv.vpn.ui.theme.dotColor
import com.kpstv.vpn.ui.theme.goldenYellowBright
import com.kpstv.vpn.ui.theme.highlightColor
import com.kpstv.vpn.ui.viewmodels.ImportViewModel
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ImportScreen(
  importViewModel: ImportViewModel = composeViewModel(),
  onItemClick: (LocalConfiguration) -> Unit,
  onPremiumClick: () -> Unit,
  isPremiumUnlocked: Boolean,
  goBack: () -> Unit
) {
  val context = LocalContext.current
  val keyboardController = LocalSoftwareKeyboardController.current

  val onItemClick = rememberUpdatedState(onItemClick)
  val goBack = rememberUpdatedState(goBack)

  val compositionCoroutineScope = rememberCoroutineScope(getContext = Dispatchers::Main)

  val localConfigurations = importViewModel.getConfigs.collectAsState(initial = emptyList())

  val onChangeProfile: (LocalConfiguration, toSave: Boolean) -> Unit =
    remember(isPremiumUnlocked, localConfigurations.value) {
      changeProfile@{ config, toSave ->
        if (!isPremiumUnlocked && localConfigurations.value.size >= MAXIMUM_LOCAL_CONFIGURATION_LIMIT) {
          keyboardController?.hide()
          Toasty.warning(context, context.getString(R.string.max_premium), Toasty.LENGTH_LONG)
            .show()
          onPremiumClick() // TODO: When premium is purchased from this screen, it doesn't unlock (problem lies with Datastore).
        } else {
          compositionCoroutineScope.launch {
            if (toSave) importViewModel.addConfig(config)
            onItemClick.value(config)
          }
        }
      }
    }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    LazyColumn {
      itemsIndexed(localConfigurations.value) { index, item ->
        if (index == 0) {
          Spacer(
            modifier = Modifier
              .statusBarsPadding()
              .height(80.dp)
          )
          ImportHeader(
            onItemClick = onChangeProfile,
            isPremiumUnlocked = isPremiumUnlocked,
            onPremiumClick = onPremiumClick
          )
          Spacer(modifier = Modifier.height(20.dp))
          Divider(color = MaterialTheme.colors.primaryVariant, thickness = 1.dp)
          Spacer(modifier = Modifier.height(15.dp))
          Text(
            text = stringResource(R.string.vpn_local_configurations),
            modifier = Modifier.padding(horizontal = 20.dp),
            style = MaterialTheme.typography.h4.copy(fontSize = 20.sp),
            color = MaterialTheme.colors.onSecondary
          )
          Spacer(modifier = Modifier.height(5.dp))
        }

        key(item.id) {
          ProfileItem(
            item = item,
            onSwipe = { config ->
              importViewModel.removeConfig(config)
            },
            onItemClick = onItemClick.value
          )
        }

        if (index == localConfigurations.value.count() - 1) {
          Spacer(
            modifier = Modifier
              .navigationBarsPadding()
              .height(15.dp)
          )
        }
      }
    }

    Column {
      Header(
        title = stringResource(R.string.import_config),
        onBackButton = goBack.value
      )
      if (localConfigurations.value.isEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        ImportHeader(
          onItemClick = onChangeProfile,
          isPremiumUnlocked = isPremiumUnlocked,
          onPremiumClick = onPremiumClick
        )
      }
    }
  }
}

@Composable
private fun ImportHeader(
  isPremiumUnlocked: Boolean,
  onItemClick: (LocalConfiguration, toSave: Boolean) -> Unit,
  onPremiumClick: () -> Unit,
) {
  ImportTipGroup(
    isPremiumUnlocked = isPremiumUnlocked,
    onPremiumClick = onPremiumClick
  )
  Profile(
    changeProfile = onItemClick
  )
}

@Composable
private fun Profile(changeProfile: (config: LocalConfiguration, save: Boolean) -> Unit) {
  val context = LocalContext.current

  val fileUri = remember { mutableStateOf<Uri?>(Uri.EMPTY) }
  val fileLocation = derivedStateOf { fileUri.value?.getFileName(context) }

  val userName = rememberSaveable { mutableStateOf("") }

  val password = rememberSaveable { mutableStateOf("") }

  val profileName = rememberSaveable { mutableStateOf("") }

  val saveProfile = remember { mutableStateOf(true) }

  val openDocumentResult =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) result@{ uri ->
      val file = uri ?: return@result
      if (VpnUtils.verifyConfigData(context, file)) {
        fileUri.value = file
      } else {
        fileUri.value = null
        Toasty.error(context, context.getString(R.string.invalid_config)).show()
      }
    }

  ProfileColumn(
    fileLocation = fileLocation.value,
    onFileChoose = {
      try {
        openDocumentResult.launch(arrayOf("application/*"))
      } catch (e: Exception) {
        Toasty.error(context, context.getString(R.string.unsupported_op)).show()
      }
    },
    userName = userName.value,
    onUserNameChanged = { userName.value = it },
    password = password.value,
    onPasswordChanged = { password.value = it },
    profileName = profileName.value,
    onProfileNameChanged = { profileName.value = it },
    saveProfile = saveProfile.value,
    onSaveProfileChanged = { saveProfile.value = it },
    onConnectToProfile = {
      if (/*userName.value.isEmpty() || password.value.isEmpty() || */profileName.value.isEmpty()) {
        Toasty.error(context, context.getString(R.string.import_error_field_profile)).show()
        return@ProfileColumn
      }
      try {
        val uri = fileUri.value ?: return@ProfileColumn

        context.contentResolver.openInputStream(uri)?.let { stream ->
          val config = stream.bufferedReader().readText()
          stream.close()
          changeProfile.invoke(
            /*config*/ LocalConfiguration(
              profileName = profileName.value,
              userName = userName.value.ifEmpty { null },
              password = password.value.ifEmpty { null },
              config = config
            ),
            /*toSave*/ saveProfile.value
          )
        }
      } catch (e: Exception) {
        Toasty.error(context, context.getString(R.string.invalid_config)).show()
      }
    }
  )
}

@Composable
private fun ProfileColumn(
  fileLocation: String?,
  onFileChoose: () -> Unit = {},
  userName: String = "",
  onUserNameChanged: (String) -> Unit = {},
  password: String = "",
  onPasswordChanged: (String) -> Unit = {},
  profileName: String = "",
  onProfileNameChanged: (String) -> Unit = {},
  saveProfile: Boolean = true,
  onSaveProfileChanged: (Boolean) -> Unit = {},
  onConnectToProfile: () -> Unit = {}
) {
  val passwordVisibility = remember { mutableStateOf(false) }

  Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) {
    // Choose a file
    Row {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(R.string.profile_select_file),
          style = MaterialTheme.typography.h5
        )
        Text(
          text = stringResource(
            R.string.profile_path,
            fileLocation ?: stringResource(R.string.profile_path_invalid)
          ),
          modifier = Modifier.padding(top = 2.dp),
          style = MaterialTheme.typography.h4.copy(fontSize = 13.sp),
          color = MaterialTheme.colors.onSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      ThemeButton(
        onClick = onFileChoose,
        modifier = Modifier.align(Alignment.CenterVertically),
        text = stringResource(R.string.choose)
      )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Username
    TextField(
      modifier = Modifier.fillMaxWidth(),
      value = userName,
      onValueChange = onUserNameChanged,
      singleLine = true,
      placeholder = {
        Text(text = stringResource(R.string.profile_username))
      }
    )

    // Password
    TextField(
      modifier = Modifier.fillMaxWidth(),
      value = password,
      onValueChange = onPasswordChanged,
      singleLine = true,
      visualTransformation = if (passwordVisibility.value) VisualTransformation.None else PasswordVisualTransformation(),
      shape = MaterialTheme.shapes.small.copy(ZeroCornerSize),
      placeholder = {
        Text(text = stringResource(R.string.profile_password))
      },
      trailingIcon = {
        IconButton(onClick = { passwordVisibility.value = !passwordVisibility.value }) {
          Icon(
            painter = painterResource(
              if (passwordVisibility.value) R.drawable.ic_baseline_visibility_off_24
              else R.drawable.ic_baseline_visibility_24
            ), contentDescription = "password visibility"
          )
        }
      }
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Profile name
    TextField(
      modifier = Modifier.fillMaxWidth(),
      value = profileName,
      onValueChange = onProfileNameChanged,
      singleLine = true,
      placeholder = {
        Text(text = "${stringResource(R.string.profile_name)} *")
      },
      colors = TextFieldDefaults.textFieldColors(
        backgroundColor = Color.Transparent
      )
    )

    Spacer(modifier = Modifier.height(20.dp))

    Row(
      modifier = Modifier.clickableNoIndication(
        onClick = { onSaveProfileChanged.invoke(!saveProfile) }
      )) {
      Checkbox(
        checked = saveProfile,
        onCheckedChange = onSaveProfileChanged
      )
      Text(
        modifier = Modifier.align(Alignment.CenterVertically),
        text = stringResource(R.string.save_profile),
        style = MaterialTheme.typography.h2.copy(fontSize = 16.sp)
      )
    }

    Spacer(modifier = Modifier.height(20.dp))

    ThemeButton(
      onClick = onConnectToProfile,
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .clip(RoundedCornerShape(10.dp)),
      text = stringResource(R.string.profile_connect)
    )
  }
}

@Composable
private fun ProfileItem(
  item: LocalConfiguration,
  onItemClick: (LocalConfiguration) -> Unit,
  onSwipe: (LocalConfiguration) -> Unit = {}
) {
  AnimatedSwipeDismiss(
    item = item,
    background = { isDismissed ->
      Box(
        modifier = Modifier
          .padding(top = 10.dp)
          .fillMaxSize()
          .background(Color.Red)
          .clip(RoundedCornerShape(10.dp))
          .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterEnd,
      ) {
        val alpha by animateFloatAsState(if (isDismissed) 0f else 1f)
        Icon(
          painter = painterResource(R.drawable.ic_delete_bin),
          tint = Color.White.copy(alpha = alpha),
          contentDescription = null
        )
      }
    },
    content = {
      Row(
        modifier = Modifier
          .padding(top = 10.dp)
          .fillMaxWidth()
          .wrapContentHeight()
          .background(MaterialTheme.colors.background)
          .padding(horizontal = 20.dp)
          .clip(RoundedCornerShape(10.dp))
          .border(
            width = 1.5.dp,
            color = dotColor.copy(alpha = 0.7f),
            shape = RoundedCornerShape(10.dp)
          )
          .clickable(onClick = { onItemClick.invoke(item) })
          .padding(13.dp)
      ) {
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.Center
        ) {
          Text(
            text = item.profileName,
            style = MaterialTheme.typography.h4.copy(fontSize = 18.sp),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
          )
          Spacer(modifier = Modifier.height(1.dp))
          Text(
            text = stringResource(
              R.string.profile_item_subtitle,
              item.userName ?: "-",
              item.password?.asPassword() ?: "-"
            ),
            style = MaterialTheme.typography.subtitle2,
            color = MaterialTheme.colors.onSurface,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
          )
        }
      }
    },
    onDismiss = { onSwipe(it) }
  )
}

@Composable
private fun ImportTipGroup(
  isPremiumUnlocked: Boolean,
  onPremiumClick: () -> Unit,
) {
  val shownTip by Settings.ImportServerTipShown.getAsState(defaultValue = !LocalInspectionMode.current)
  ImportServerQuickTip()
  PremiumImportQuickTip(isPremiumUnlocked = isPremiumUnlocked, onPremiumClick = onPremiumClick)
  AnimatedVisibility(visible = !shownTip && !isPremiumUnlocked) {
    Spacer(modifier = Modifier.height(15.dp))
  }
}

@Composable
private fun ImportServerQuickTip() {
  val context = LocalContext.current
  val shownTip by Settings.ImportServerTipShown.getAsState(defaultValue = !LocalInspectionMode.current)
  Box(modifier = Modifier.padding(horizontal = 20.dp)) {
    QuickTip(
      message = stringResource(R.string.import_server_tip_text),
      visible = !shownTip,
      button = {
        ThemeButton(
          onClick = {
            Settings.ImportServerTipShown.set(true)
            context.launchUrlInApp(context.getString(R.string.app_import_server))
          },
          text = stringResource(R.string.learn_more)
        )
      }
    )
  }
}

@Composable
private fun PremiumImportQuickTip(isPremiumUnlocked: Boolean, onPremiumClick: () -> Unit) {
  val importTipShown by Settings.ImportServerTipShown.getAsState(defaultValue = LocalInspectionMode.current)
  Box(modifier = Modifier.padding(horizontal = 20.dp)) {
    QuickTip(
      message = stringResource(R.string.premium_import_tip_text, MAXIMUM_LOCAL_CONFIGURATION_LIMIT),
      visible = importTipShown && !isPremiumUnlocked,
      cardColor = highlightColor,
      textColor = MaterialTheme.colors.surface.copy(alpha = 0.8f),
      button = {
        ThemeButton(
          backgroundColor = goldenYellowBright,
          textColor = MaterialTheme.colors.surface,
          onClick = onPremiumClick,
          text = stringResource(R.string.buy_premium)
        )
      }
    )
  }
}

private const val MAXIMUM_LOCAL_CONFIGURATION_LIMIT = 3

@Preview
@Composable
fun PreviewProfileScreen() {
  CommonPreviewTheme {
    ProfileColumn(
      fileLocation = null,
    )
  }
}

@Preview
@Composable
fun PreviewProfileItem() {
  CommonPreviewTheme {
    ProfileItem(
      item = LocalConfiguration(
        profileName = "Test Profile",
        userName = "vpn",
        password = "vpn",
        config = ""
      ),
      onItemClick = {}
    )
  }
}

@Preview
@Composable
fun PreviewImportServerQuickTip() {
  CommonPreviewTheme {
    ImportServerQuickTip()
  }
}

@Preview
@Composable
fun PreviewPremiumImportServerQuickTip() {
  CommonPreviewTheme {
    PremiumImportQuickTip(isPremiumUnlocked = false, onPremiumClick = {})
  }
}