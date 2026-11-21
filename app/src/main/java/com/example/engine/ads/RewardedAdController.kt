package com.example.engine.ads

import java.util.UUID

/**
 * Controls Rewarded Ad session state, verification nonce/idempotency key generation,
 * and server-verified +2 credit rewards (only upon full completion).
 */
class RewardedAdController {
    companion object {
        // Google AdMob official Android Rewarded Ad test unit ID
        const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        const val REWARD_DURATION_SECONDS = 5
        const val REWARD_CREDITS_AMOUNT = 2L
    }

    fun generateIdempotencyKey(): String =
        "rwd_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
}
