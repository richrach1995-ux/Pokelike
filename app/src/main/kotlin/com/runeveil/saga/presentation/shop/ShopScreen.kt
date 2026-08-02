package com.runeveil.saga.presentation.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.runeveil.saga.R
import com.runeveil.saga.audio.AudioEngine
import com.runeveil.saga.audio.AudioKeys
import com.runeveil.saga.domain.model.item.Item
import com.runeveil.saga.domain.repository.ContentRepository
import com.runeveil.saga.domain.repository.PlayerRepository
import com.runeveil.saga.domain.rules.InventoryRules
import com.runeveil.saga.domain.usecase.TradeUseCase
import com.runeveil.saga.ui.components.EmptyState
import com.runeveil.saga.ui.components.RunePanel
import com.runeveil.saga.ui.components.RunicOutlinedButton
import com.runeveil.saga.ui.components.contentText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A trading post. Prices already include the regional markup. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    shopId: String,
    onBack: () -> Unit,
    viewModel: ShopViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(shopId) { viewModel.load(shopId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.world_shop)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                actions = {
                    Text(
                        text = stringResource(R.string.common_gold, state.gold),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 14.dp),
                    )
                },
            )
        },
    ) { padding ->
        if (state.offers.isEmpty()) {
            EmptyState(stringResource(R.string.common_loading), Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            items(state.offers, key = { it.item.id }) { offer ->
                RunePanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                contentText(offer.item.nameKey),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                stringResource(R.string.common_gold, offer.price),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            contentText(offer.item.descriptionKey),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        RunicOutlinedButton(
                            text = stringResource(R.string.shop_buy),
                            onClick = { viewModel.buy(offer.item.id) },
                            enabled = state.gold >= offer.price,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

data class ShopOfferUi(val item: Item, val price: Int)
data class ShopUiState(
    val offers: List<ShopOfferUi> = emptyList(),
    val gold: Int = 0,
    val shopId: String = "",
)

@HiltViewModel
class ShopViewModel @Inject constructor(
    private val content: ContentRepository,
    private val player: PlayerRepository,
    private val trade: TradeUseCase,
    private val audio: AudioEngine,
) : ViewModel() {

    private val _state = MutableStateFlow(ShopUiState())
    val state: StateFlow<ShopUiState> = _state.asStateFlow()

    fun load(shopId: String) {
        viewModelScope.launch {
            content.ensureLoaded()
            val shop = content.shop(shopId) ?: return@launch
            val offers = shop.itemIds.mapNotNull { itemId ->
                content.item(itemId)?.let {
                    ShopOfferUi(it, InventoryRules.buyPrice(it, shop.markup, shop.factionDiscountPercent))
                }
            }
            _state.value = ShopUiState(offers, player.profile().gold, shopId)
        }
    }

    fun buy(itemId: String) {
        viewModelScope.launch {
            if (trade.buy(_state.value.shopId, itemId, 1)) {
                audio.playSound(AudioKeys.PURCHASE)
            } else {
                audio.playSound(AudioKeys.UI_ERROR)
            }
            _state.value = _state.value.copy(gold = player.profile().gold)
        }
    }
}
