<template>
  <router-view />
  <GlobalAlert
    :isVisible="isVisible"
    :warningMsg="currentWarning"
    :articleId="currentArticleId"
    @close="closeAlert"
    @view-truth="viewTruth"
  />
  <div class="debug-bar" v-if="lastReceivedText || appStatus">
    <div class="debug-item" v-if="appStatus">
      <span class="debug-label">App状态：</span>{{ appStatus }}
    </div>
    <div class="debug-item" v-if="lastReceivedText">
      <span class="debug-label">识别文字：</span>"{{ lastReceivedText }}"
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import request from './api/request.js'
import GlobalAlert from './components/GlobalAlert.vue'

const router = useRouter()

const isVisible = ref(false)
const currentWarning = ref('')
const currentArticleId = ref(0)

function closeAlert() {
  isVisible.value = false
}

function viewTruth() {
  isVisible.value = false
  if (currentArticleId.value) {
    router.push('/article/' + currentArticleId.value)
  }
}

const lastReceivedText = ref('')
const appStatus = ref('')

window.receiveAudioText = async function (text) {
  lastReceivedText.value = text
  try {
    const res = await request.post('/api/v1/analysis', { text })
    const data = res.data.data
    if (data.isFraud) {
      currentWarning.value = data.warningMessage
      currentArticleId.value = data.targetArticleId
      isVisible.value = true
      // 同时发送系统通知（用户在别的 App 时也能看到）
      if (window.AndroidApp) {
        window.AndroidApp.showFraudAlert(data.warningMessage, data.targetArticleId)
      }
    }
  } catch (e) {
    console.error('分析请求失败', e)
  }
}

window.onAppStatus = function (msg) {
  appStatus.value = msg
}

// 供 Android 通知点击时调用，跳转到指定文章（不再重复弹全屏警告）
window.routerPush = function (path) {
  isVisible.value = false
  router.push(path)
}
</script>

<style>
.debug-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 9998;
  background: rgba(0, 0, 0, 0.85);
  padding: 16px 20px;
  padding-bottom: calc(16px + env(safe-area-inset-bottom, 8px));
}

.debug-item {
  font-size: 18px;
  color: #0f0;
  line-height: 1.6;
  word-break: break-all;
}

.debug-label {
  color: #ff0;
  font-weight: 700;
}
</style>
