package com.apple.small.cardmatchgame.activity;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.apple.small.cardmatchgame.R;
import com.apple.small.cardmatchgame.Utils.DelayAction;
import com.apple.small.cardmatchgame.bean.CardBean;

import java.util.ArrayList;
import java.util.Collections;

public class CardMatchActivity extends AppCompatActivity implements  Handler.Callback {
    private Handler mHdler;

    private void initHandler() {
        if (mHdler==null) {
            mHdler = new Handler(Looper.getMainLooper(),this);
        }
    }
    // 提交异步任务
    private void commitAction(int id,int arg,Object extra,int ms) {
        if (mHdler==null) {
            initHandler();
        }
        mHdler.sendMessageDelayed(Message.obtain(mHdler,id,arg,0,extra),ms);
    }
    // 根据id移除
    private void removeActionById(int id) {
        if (mHdler!=null) {
            mHdler.removeMessages(id);
        }
    }

    private static final int IA_FLOP_BACK = 0;// 卡牌匹配，翻开一张牌后无操作三秒后自动翻回
    private static final int FLOP_BACK_ALL = 1;// 1 初次展示卡牌，两秒后翻回背面
    private static final int FLOP_BACK_ONE = 2;// 2 翻开一张卡牌超过三秒未操作则重新翻回去
    private static final int ONCE_DONE = 3;
    private static final int SOUND_CORRECT = 4;
    private static final int SOUND_WRONG = 5;
    public static final int CARD_MEMBER_NUM = 2;

    private int mClickFlag = 0x0; // 0x8=全部点击过
    // view
    private RelativeLayout mRlCardView;
    // 翻牌动画
    private Animator[] mAnimOutSet;
    private Animator[] mAnimInSet;
    // 正误提示动画
    private AlphaAnimation mAnimFailed;
    private Animation mAnimSuccess;
    // 数据和状态值
    private int cardNum = 0;//卡牌数目
    private boolean isStart = false;
    private CardViewHolder selCardView1 = null, selCardView2 = null;
    private ArrayList<Integer> indexList = new ArrayList<>();

    private ArrayList<CardBean> cardList = new ArrayList<>();
    private MediaPlayer mediaPlayer;
    private DelayAction delayAction = new DelayAction();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initData();
        loadAnim();
    }

    private void initView() {
        View mRootView = View.inflate(this, R.layout.card_match_view, null);
        mRlCardView = (RelativeLayout) mRootView.findViewById(R.id.card_list);
        setContentView(mRootView);
    }

    /**
     * 页面及卡牌信息初始化
     */
    private void initData() {
        initCardList();
        delayAction.setInner(600);
        cardNum = cardList.size() * CARD_MEMBER_NUM;//卡牌数目
        mAnimOutSet = new Animator[cardNum];
        mAnimInSet = new Animator[cardNum];
        // 初始状态，即展示过程中，不可点
        isStart = false;
        // 标题
        initIndexList();    // 初始化乱序数组
        // 初始化卡牌view
        initCardViewLayout();
        // 初始状态展示卡牌正面，两秒后翻回背面
        commitAction(IA_FLOP_BACK, FLOP_BACK_ALL, null, 2000);
    }

    private void initCardList() {
        cardList.clear();
        for (int i=0; i<4; i++) {
            CardBean cardBean = new CardBean();
            cardBean.imgName_first = "img"+i+"_first";
            cardBean.imgName_second = "img"+i+"_second";
            cardList.add(cardBean);
        }
    }

    /**
     * 初始化卡牌布局
     * 动态加载单张卡牌元素
     */
    private void initCardViewLayout() {
        mRlCardView.removeAllViews();
        for (int i = 0; i < cardNum; i++) {
            View cardView = LayoutInflater.from(this).inflate(R.layout.card_view_item, null);
            RelativeLayout.LayoutParams cardLp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            String idStr = "id_card_view" + i;
            int cardViewId = this.getResources().getIdentifier(idStr, "id", this.getPackageName());
            cardView.setId(cardViewId);
            if (i > 0 && i % 2 == 0) {
                int aboveId = mRlCardView.getChildAt(i - 2).getId();
                cardLp.addRule(RelativeLayout.BELOW, aboveId);
                cardLp.addRule(RelativeLayout.ALIGN_LEFT, aboveId);
                cardLp.topMargin = px(20);
            } else if (i > 0 && i % 2 > 0) {
                int leftId = mRlCardView.getChildAt(i - 1).getId();
                cardLp.addRule(RelativeLayout.RIGHT_OF, leftId);
                cardLp.addRule(RelativeLayout.ALIGN_BOTTOM, leftId);
                cardLp.leftMargin = px(20);
            }
            cardView.setLayoutParams(cardLp);
            mRlCardView.addView(cardView);
            CardViewHolder holder = new CardViewHolder(cardView);
            bindCardViewHolder(holder, i);
            cardView.setTag(holder);
            setCardViewClickable(i);
        }
    }

    /**
     * 绑定卡牌数据
     * @param holder
     * @param position
     */
    private void bindCardViewHolder(final CardViewHolder holder, final int position) {
        if (cardList == null || position >= cardList.size() * CARD_MEMBER_NUM) {
            return;
        }
        // 获取实际对象信息
        final int currPosition = indexList.get(position);  // 获取元素下标位置分组,每个对象包含一组元素
        holder.position = position;
        holder.realIndex = currPosition / CARD_MEMBER_NUM; // 对象实际对应下标
        holder.isFirst = currPosition % CARD_MEMBER_NUM != 0;
        CardBean realCardBean = cardList.get(holder.realIndex);
        String imgStr = holder.isFirst ? realCardBean.imgName_first : realCardBean.imgName_second;
        int cardViewId = this.getResources().getIdentifier(imgStr, "mipmap", this.getPackageName());
        holder.mIvImage.setImageResource(cardViewId);
        // 点击背景 卡牌翻转
        holder.mRlBg.setTag(holder);
        // 点击图片正面 单词卡牌发音
        holder.mIvImage.setTag(holder);
        setCameraDistance(holder.mRlBg, holder.mIvImage);
    }

    /**
     * 改变视角距离, 贴近屏幕
     *
     * @param view1
     * @param view2
     */
    private void setCameraDistance(View view1, View view2) {
        int distance = 16000;
        float scale = px(distance);
        view1.setCameraDistance(scale);
        view2.setCameraDistance(scale);
    }

    /**
     * 初始化乱序数组
     */
    private void initIndexList() {
        if (cardNum <= 0)
            return;
        this.indexList.clear();
        for (int i = 0; i < cardNum; i++) {
            this.indexList.add(i);
        }
        Collections.shuffle(this.indexList);
    }

    /**
     * 加载动画
     */
    private void loadAnim() {
        // 左右翻牌动画
        for (int pos = 0; pos < cardNum; pos++) {
            mAnimOutSet[pos] = AnimatorInflater.loadAnimator(this, R.animator.flop_match_out);
            mAnimInSet[pos] = AnimatorInflater.loadAnimator(this, R.animator.flop_match_in);
            final int finalPos = pos;
            mAnimInSet[pos].addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    setCardViewClickable(finalPos);
                    if (isStart) {
                        matchCard(finalPos);
                    }
                }
            });
        }
        // 匹配错误闪烁动画
        mAnimFailed = (AlphaAnimation) AnimationUtils.loadAnimation(this, R.anim.twinkle_rect_failed);
        mAnimFailed.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (selCardView1 == null || selCardView2 == null) return;
                // 匹配失败，卡牌重新翻转回来
                flipCard(selCardView1.mIvImage, selCardView1.mRlBg, selCardView1.position);
                flipCard(selCardView2.mIvImage, selCardView2.mRlBg, selCardView2.position);
                selCardView1.isShowing = false;
                selCardView2.isShowing = false;
                initSelCardView();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

        });
        // 匹配正确闪烁动画
        mAnimSuccess = AnimationUtils.loadAnimation(this, R.anim.twinkle_rect_success);
        mAnimSuccess.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                initSelCardView();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    private void setCardViewClickable(int position) {
        CardViewHolder cardViewHolder = getCardView(position);
        if (cardViewHolder == null) {
            return;
        }
        if (cardViewHolder.isShowing) {
            cardViewHolder.mIvImage.setClickable(true);
            cardViewHolder.mRlBg.setClickable(false);
        } else {
            cardViewHolder.mIvImage.setClickable(false);
            cardViewHolder.mRlBg.setClickable(true);
        }
    }

    /**
     * 获取当前操作的卡牌视图
     *
     * @param position
     * @return
     */
    private CardViewHolder getCardView(int position) {
        CardViewHolder cardViewHolder = null;
        View view = mRlCardView.getChildAt(position);
        if (null != view) {
            cardViewHolder = (CardViewHolder) view.getTag();
        }
        return cardViewHolder;
    }

    /**
     * 初始化已翻开的卡牌位置列表
     */
    private void initSelCardView() {
        selCardView1 = null;
        selCardView2 = null;
    }

    /**
     * 匹配卡牌并播放正误提示动画
     */
    private void matchCard(int currPosition) {
        // 已翻开两张卡牌且当前index为第二张翻开的卡牌的index，才继续匹配
        if (selCardView1 == null || selCardView2 == null || selCardView2.position != currPosition) {
            if (selCardView2 == null && selCardView1 != null) {
                commitAction(IA_FLOP_BACK, FLOP_BACK_ONE, currPosition, 3000);
            }
            return;
        }
        boolean isMatched = selCardView1.realIndex == selCardView2.realIndex;
        if (isMatched) {
            mClickFlag = mClickFlag | (0x1 << selCardView1.realIndex);
        }
        // 展示卡牌匹配结果
        playMatchResAnim(selCardView1.mIvRect, isMatched);
        playMatchResAnim(selCardView2.mIvRect, isMatched);
    }

    /**
     * 播放选中结果提示动画，即红框绿框
     *
     * @param view      边框view
     * @param isMatched 是否匹配成功
     */
    private void playMatchResAnim(ImageView view, boolean isMatched) {
        view.setVisibility(View.VISIBLE);
        if (isMatched) {
            view.setImageResource(R.drawable.btn_rectangle_green);
            view.startAnimation(mAnimSuccess);
            partlyDone(true); // 本次匹配成功
        } else {
            view.setImageResource(R.drawable.btn_rectangle_red);
            view.startAnimation(mAnimFailed);
            partlyDone(false); // 本次匹配失败
        }
    }

    private boolean checkEnd() {
        return mClickFlag == (0x1 << cardList.size()) - 1;
    }

    /**
     * 完成一部分，比如卡牌游戏匹配一对
     *
     * @param isCorrect 该部分是否正确
     */
    private void partlyDone(boolean isCorrect) {
        if (isCorrect) {
            commitAction(ONCE_DONE, SOUND_CORRECT, null, 0);
        } else {
            commitAction(ONCE_DONE, SOUND_WRONG, null, 0);
        }
    }

    // 翻转卡片
    private void flipCard(View view1, View view2, int pos) {
        if (pos < 0) {
            return;
        }
        delayAction.valid();
        removeActionById(IA_FLOP_BACK);
        view1.setClickable(false);
        view2.setClickable(false);

        mAnimOutSet[pos].setTarget(view1);
        mAnimInSet[pos].setTarget(view2);
        mAnimOutSet[pos].start();
        mAnimInSet[pos].start();

    }

    private static class CardViewHolder {
        private ImageView mIvImage;
        private RelativeLayout mRlBg;
        private ImageView mIvRect;
        private int realIndex;
        private boolean isFirst;
        private int position = -1;
        private boolean isShowing = true;

        private CardViewHolder(View itemView) {
            mIvImage = (ImageView) itemView.findViewById(R.id.mIvImage);
            mRlBg = (RelativeLayout) itemView.findViewById(R.id.mRlBg);
            mIvRect = (ImageView) itemView.findViewById(R.id.rect_iv);
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case IA_FLOP_BACK:
                if (msg.arg1 == FLOP_BACK_ALL) {
                    delayAction.valid();
                    flipBackAllCards();
                } else if (msg.arg1 == FLOP_BACK_ONE) {
                    if (msg.obj instanceof Integer) {
                        CardViewHolder cardView = getCardView((Integer) msg.obj);
                        cardView.isShowing = false;
                        flipCard(cardView.mIvImage, cardView.mRlBg, (Integer) msg.obj);
                        initSelCardView();
                    }
                }
                break;
            case ONCE_DONE://完成一部分
                //完成一部分，播放正确或错误音频
                playAnswerVoice(msg.arg1 == SOUND_CORRECT);
                break;
            default:
                break;
        }
        return true;
    }

    public void onClick(View view) {
        CardViewHolder cardViewHolder = (CardViewHolder) view.getTag();
        isStart = true;
        // 判断是否可选,不能重复选择，已经选了两张图片不能重复添加
        if (!isAddAllowed() || delayAction.invalid()) {
            return;
        }
        selCard(cardViewHolder);
        cardViewHolder.isShowing = true;
        flipCard(cardViewHolder.mRlBg, cardViewHolder.mIvImage, cardViewHolder.position);
    }

    //播放答题对错的音频
    private void playAnswerVoice(boolean correct) {
        releaseAudio();
        if (checkEnd()) {
            mediaPlayer= MediaPlayer.create(this, R.raw.win_sound);
        } else if (correct) {
            mediaPlayer= MediaPlayer.create(this, R.raw.correct_sound);
        } else {
            mediaPlayer= MediaPlayer.create(this, R.raw.wrong_sound);
        }
        mediaPlayer.start();

    }

    private void releaseAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            }
    }

    /**
     * 所有卡牌翻转回背面
     */
    private void flipBackAllCards() {
        if (cardNum <= 0)
            return;
        for (int pos = 0; pos < cardNum; pos++) {
            CardViewHolder cardView = getCardView(pos);
            cardView.isShowing = false;
            flipCard(cardView.mIvImage, cardView.mRlBg, pos);
        }
        initSelCardView();
    }

    /**
     * 记录已选卡牌
     *
     * @param cardViewHolder 已选卡牌的viewholder
     */
    private void selCard(CardViewHolder cardViewHolder) {
        if (selCardView1 == null) {
            selCardView1 = cardViewHolder;
        } else if (selCardView2 == null) {
            selCardView2 = cardViewHolder;
        }
    }

    /**
     * 是否允许继续选择卡牌
     *
     * @return
     */
    private boolean isAddAllowed() {
        return !(selCardView1 != null && selCardView2 != null);
    }

    public int px(float dp) {
        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        int result = (int)(dp*metrics.density+0.5f);
        return result > 0 ? result : 1;
    }

    @Override
    protected void onDestroy() {
        releaseAudio();
        super.onDestroy();
    }
}
