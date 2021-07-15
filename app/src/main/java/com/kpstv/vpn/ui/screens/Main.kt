package com.kpstv.vpn.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.coil.rememberCoilPainter
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.statusBarsPadding
import com.kpstv.vpn.R
import com.kpstv.vpn.data.models.VpnConfiguration
import com.kpstv.vpn.extensions.utils.FlagUtils
import com.kpstv.vpn.ui.components.*
import com.kpstv.vpn.ui.theme.CommonPreviewTheme
import com.kpstv.vpn.ui.theme.cyanDark
import com.kpstv.vpn.ui.theme.greenColorDark
import com.kpstv.vpn.ui.theme.purpleColor

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
  publicIp: String?,
  configuration: VpnConfiguration = VpnConfiguration.createEmpty(),
  connectivityStatus: ConnectivityStatus = ConnectivityStatus.NONE,
  onChangeServer: () -> Unit = {},
  onConnectClick: () -> Unit = {},
  onDisconnect: () -> Unit = {},
  suppressBackPress: (Boolean) -> Unit = {}
) {
  val premiumBottomSheet = rememberBottomSheetState()

  val ipTextColor: Color by animateColorAsState(
    if (connectivityStatus == ConnectivityStatus.CONNECTED) greenColorDark else MaterialTheme.colors.error
  )

  val ipText = if (connectivityStatus != ConnectivityStatus.CONNECTED)
    stringResource(R.string.vpn_status, publicIp ?: stringResource(R.string.vpn_public_ip_unknown))
  else stringResource(R.string.vpn_status_hidden)

  Column(
    modifier = Modifier
      .padding(top = 5.dp)
      .statusBarsPadding()
      .navigationBarsPadding()
      .fillMaxSize()
  ) {
    Box(modifier = Modifier.fillMaxWidth()) {
      Text(
        text = stringResource(R.string.app_name),
        modifier = Modifier
          .align(Alignment.Center)
          .padding(top = 5.dp),
        style = MaterialTheme.typography.h4
      )
      IconButton(
        onClick = { premiumBottomSheet.value = BottomSheetState.Expanded },
        modifier = Modifier
          .padding(end = 10.dp)
          .align(Alignment.CenterEnd)
      ) {
        Image(painter = painterResource(R.drawable.ic_crown), contentDescription = "Get premium")
      }
    }

    Spacer(modifier = Modifier.weight(0.5f))

    CircularBox(status = connectivityStatus)

    Spacer(modifier = Modifier.padding(top = 30.dp))

    Text(
      text = stringResource(R.string.vpn_public_ip),
      modifier = Modifier.align(Alignment.CenterHorizontally),
      style = MaterialTheme.typography.subtitle1,
      color = MaterialTheme.colors.onSecondary
    )

    Text(
      text = ipText,
      modifier = Modifier
        .align(Alignment.CenterHorizontally)
        .padding(top = 3.dp)
        .animateContentSize(),
      style = MaterialTheme.typography.subtitle1,
      color = ipTextColor,
    )

    Spacer(modifier = Modifier.weight(1f))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(15.dp)
        .height(90.dp)
        .border(
          border = BorderStroke(
            width = 2.dp,
            brush = Brush.horizontalGradient(
              0.5f to cyanDark, 1f to purpleColor
            )
          ),
          shape = RoundedCornerShape(10.dp)
        )
        .padding(10.dp)
    ) {
      AnimatedVisibility(visible = configuration.countryFlagUrl.isNotEmpty()) {
        Image(
          painter = rememberCoilPainter(
            FlagUtils.getOrNull(configuration.country) ?: configuration.countryFlagUrl
          ),
          modifier = Modifier
            .padding(10.dp)
            .requiredWidthIn(max = 50.dp)
            .fillMaxHeight(),
          contentDescription = "country",
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      Column(
        modifier = Modifier
          .weight(1f)
          .align(Alignment.CenterVertically)
      ) {
        Text(
          text = configuration.country,
          style = MaterialTheme.typography.h2,
          overflow = TextOverflow.Ellipsis,
          maxLines = 1
        )
        Text(
          text = configuration.ip,
          style = MaterialTheme.typography.subtitle2,
          color = MaterialTheme.colors.onSecondary
        )
      }

      Spacer(modifier = Modifier.padding(start = 10.dp))

      ThemeButton(
        onClick = onChangeServer,
        modifier = Modifier
          .fillMaxHeight()
          .clip(RoundedCornerShape(5.dp)),
        text = stringResource(R.string.change_server),
        enabled = connectivityStatus != ConnectivityStatus.CONNECTED && connectivityStatus != ConnectivityStatus.CONNECTING
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(5.dp)
        .padding(horizontal = 32.dp)
        .height(55.dp)
    ) {
      ThemeButton(
        enabled = connectivityStatus != ConnectivityStatus.CONNECTING && configuration.isNotEmpty(),
        onClick = {
          if (connectivityStatus == ConnectivityStatus.CONNECTED) {
            onDisconnect.invoke()
          } else {
            onConnectClick.invoke()
          }
        },
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(10.dp))
          .fillMaxHeight()
          .animateContentSize(),
        text = if (connectivityStatus != ConnectivityStatus.CONNECTED)
          stringResource(R.string.status_connect)
        else stringResource(R.string.status_disconnect)
      )

      AnimatedVisibility(visible = connectivityStatus == ConnectivityStatus.CONNECTING) {
        ThemeButton(
          onClick = onDisconnect,
          modifier = Modifier
            .padding(start = 20.dp)
            .clip(RoundedCornerShape(10.dp))
            .fillMaxHeight(),
          text = stringResource(R.string.stop)
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))
  }

  PremiumBottomSheet(premiumBottomSheet) { suppressBackPress(it) }
}

@Preview(showBackground = true)
@Composable
fun PreviewStartScreen() {
  CommonPreviewTheme {
    MainScreen(publicIp = "104.156.232.238")
  }
}