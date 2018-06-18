package com.apple.small.cardmatchgame.Utils;

import android.os.SystemClock;

// 演示操作 防止用户频繁点击导致问题
public class DelayAction {
	private long mLastEvt = 0; // 防止频繁点击
	private int mTimeInner = 250; // 点击间隔 默认500ms
	public DelayAction() {
	}
	// 设置延时时间间隔
	public DelayAction setInner(int ms) {
		mTimeInner = ms;
		return this;
	}
	// 检查是否有效事件
	public boolean invalid() {
		return !valid();
	}
	// 检查是否有效事件
	public boolean valid() {
		long cur  = SystemClock.uptimeMillis();
		// 修改过系统时间导致小于
		if (mLastEvt==0||cur>(mLastEvt+mTimeInner)||cur<mLastEvt) {
			mLastEvt = cur;
			return true;
		}
		return false;
	}
	// 清理
	public void clear() {
		mLastEvt = 0;
	}
}
