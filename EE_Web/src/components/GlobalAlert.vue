<template>
  <div class="alert-overlay" v-if="isVisible" @click.self="$emit('close')">
    <div class="alert-box">
      <div class="alert-icon">⚠️</div>
      <p class="alert-msg">{{ warningMsg }}</p>
      <p class="alert-sub">您可能正在面临诱导性推销或欺诈，请谨慎。点击此处查看科学处理方法。</p>
      <button class="alert-btn" @click="$emit('view-truth')">查看防骗真相</button>
      <button class="alert-dismiss" @click="$emit('close')">我知道了</button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  isVisible: Boolean,
  warningMsg: String,
  articleId: Number,
})

defineEmits(['close', 'view-truth'])
</script>

<style scoped>
.alert-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  width: 100dvw;
  height: 100%;
  height: 100dvh;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.75);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 32px 20px;
  padding-bottom: calc(32px + env(safe-area-inset-bottom, 20px));
}

.alert-box {
  background: #d32f2f;
  border-radius: 24px;
  padding: 40px 28px;
  max-width: 380px;
  width: 100%;
  text-align: center;
  box-shadow: 0 8px 40px rgba(211, 47, 47, 0.5);
}

.alert-icon {
  font-size: 56px;
  margin-bottom: 20px;
}

.alert-msg {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  line-height: 1.6;
  margin-bottom: 16px;
  word-break: break-word;
}

.alert-sub {
  font-size: 22px;
  color: rgba(255, 255, 255, 0.9);
  line-height: 1.5;
  margin-bottom: 28px;
  word-break: break-word;
}

.alert-btn {
  display: block;
  width: 100%;
  font-size: 26px;
  font-weight: 700;
  color: #d32f2f;
  background: #fff;
  border: none;
  border-radius: 14px;
  padding: 20px;
  cursor: pointer;
  margin-bottom: 18px;
  min-height: 56px;
}

.alert-btn:active {
  background: #eee;
}

.alert-dismiss {
  font-size: 22px;
  color: rgba(255, 255, 255, 0.8);
  background: transparent;
  border: 3px solid rgba(255, 255, 255, 0.5);
  border-radius: 14px;
  padding: 16px;
  cursor: pointer;
  width: 100%;
  min-height: 52px;
}

.alert-dismiss:active {
  background: rgba(255, 255, 255, 0.1);
}
</style>
