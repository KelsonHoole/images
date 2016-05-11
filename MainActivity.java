package cn.hukecn.speechbrowser.activity;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;

import cn.hukecn.speechbrowser.R;
import cn.hukecn.speechbrowser.Shake;
import cn.hukecn.speechbrowser.Shake.ShakeListener;
import cn.hukecn.speechbrowser.DAO.MyDataBase;
import cn.hukecn.speechbrowser.DAO.MySharedPreferences;
import cn.hukecn.speechbrowser.bean.BookMarkBean;
import cn.hukecn.speechbrowser.bean.HistoryBean;
import cn.hukecn.speechbrowser.bean.HtmlBean;
import cn.hukecn.speechbrowser.bean.MailBean;
import cn.hukecn.speechbrowser.bean.MailListBean;
import cn.hukecn.speechbrowser.bean.NewsBean;
import cn.hukecn.speechbrowser.extractor.Extractor;
import cn.hukecn.speechbrowser.extractor.News;
import cn.hukecn.speechbrowser.location.BaseAppLocation;
import cn.hukecn.speechbrowser.util.BaiduSearch;
import cn.hukecn.speechbrowser.util.JsonParser;
import cn.hukecn.speechbrowser.util.ParseCommand;
import cn.hukecn.speechbrowser.util.ParseFengNews;
import cn.hukecn.speechbrowser.util.ParseMailContent;
import cn.hukecn.speechbrowser.util.ParseMailList;
import cn.hukecn.speechbrowser.util.ParsePageType;
import cn.hukecn.speechbrowser.util.ParseTencentNews;
import cn.hukecn.speechbrowser.util.ParseWeatherHtml;
import cn.hukecn.speechbrowser.util.ToastUtil;
import cn.hukecn.speechbrowser.util.Trans2PinYin;
import cn.hukecn.speechbrowser.util.ViewPageAdapter;
import cn.hukecn.speechbrowser.view.NumberProgressBar;
import cn.hukecn.speechbrowser.view.SuperWebView;
import cn.hukecn.speechbrowser.view.SuperWebView.CutWebCallback;
import cn.hukecn.speechbrowser.view.SuperWebView.ReceiveHtmlListener;
import cn.hukecn.speechbrowser.view.SuperWebView.ReceiveMessageListener;
import cn.hukecn.speechbrowser.view.EditUrlPopupWindow;
import cn.hukecn.speechbrowser.view.EditUrlPopupWindow.EditUrlPopupDismissListener;
import cn.hukecn.speechbrowser.view.MenuPopupWindow;

import com.baidu.location.BDLocation;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.widget.ImageButton;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends Activity implements ShakeListener
			,OnClickListener,CutWebCallback
			,ReceiveHtmlListener,ReceiveMessageListener{
	public final int REQUEST_CODE_BOOKMARK = 1;
	public final int REQUEST_CODE_HISTORY = 2;
	public final int REQUEST_CODE_SETTING = 3;
//	BDLocation location;
	MenuPopupWindow popWindow;
	List<Integer> cmdList = new ArrayList<Integer>();
	private SoundPool sp;//����һ��SoundPool
	private int musicStart;//����һ��������load������������suondID
	private int musicEnd;
	private int currentIndex = -1;
	private static Vibrator mVibrator;
	private HtmlBean htmlBean = new HtmlBean();
	private NumberProgressBar webviewProgressBar = null;
	private NumberProgressBar speechProgressBar = null;
	boolean isPause = false;
	int btntate = 0;//0������ʼ��1������ͣ��2����ֹͣ
	TextView tv_head = null;
	ImageButton btn_menu = null,
			btn_left = null,
			btn_right = null,
			btn_state = null;
	ImageButton btn_microphone = null;
	long lastTime = 0l;
	long lastShakeTime = 0l;
//	int mailListCount = 0;
	String mailCookie = "";
	String msid = "";
	// ������д����
	//private SpeechRecognizer mIat;
	// ������дUI
	private RecognizerDialog mIatDialog;
//	TextView title = null;
	TextView tv_info = null;
	SpeechSynthesizer mTts;
	List<NewsBean> newsList = null;
	List<MailListBean> mailList = null;
	List<BookMarkBean> bookmarkList = null;
	SuperWebView webViewMain = null;
	RelativeLayout rl_head = null;
	ViewPager mViewPager = null;
	ViewPageAdapter pageAdapter = null;
	private boolean blind = false;
	private boolean autoread = false;
	private boolean shake = true;
	private boolean saving = false;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
	         getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
	    }
		
		final SharedPreferences sharedpref = MySharedPreferences.getInstance(getApplicationContext());
	    autoread = sharedpref.getBoolean("autoread", false);
	    blind = sharedpref.getBoolean("blind", false);
	    shake = sharedpref.getBoolean("shake", true);
	    saving = sharedpref.getBoolean("saving", false);
	    
		initSpeechUtil();
		initView();
		
		sp= new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
		musicStart = sp.load(this, R.raw.shake, 1);
		musicEnd = sp.load(this, R.raw.bdspeech_recognition_success,1);
	
        mVibrator = (Vibrator)getSystemService(Service.VIBRATOR_SERVICE);  
        Intent intent = getIntent();
        if(intent != null)
        {
        	String action = intent.getAction();
        	if(action != null)
	        	if (action.equals("android.intent.action.VIEW")) 
	        	{
					Uri uri = intent.getData();
					String url = uri.toString();
					if(url != null && url.length() > 0)
					{
						webViewMain.loadUrl(url);
						mViewPager.setCurrentItem(1);
						return;
					}
				}
       	}
//		webViewMain.loadUrl("http://m.baidu.com");
	}
	private void initView() {
		// TODO Auto-generated method stub
		mViewPager = (ViewPager) findViewById(R.id.viewpager);
		View view0 = View.inflate(this, R.layout.page_layout_webview, null);
		View view1 = View.inflate(this, R.layout.page_layout_webview, null);
		View view2 = View.inflate(this, R.layout.page_layout_textview, null);
		
		List<View> viewList = new ArrayList<View>();
		viewList.add(view0);
		viewList.add(view1);
		viewList.add(view2);
		String[] titles = {"��ҳ","��ҳ","����"};
		pageAdapter = new ViewPageAdapter(viewList, titles);
		mViewPager.setAdapter(pageAdapter);
		mViewPager.setCurrentItem(0);
		mViewPager.addOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageSelected(int arg0) {
				// TODO Auto-generated method stub
				if(arg0 == 2)
				{
					webViewMain.loadUrl("javascript:window.HTML.getHtml(document.getElementsByTagName('html')[0].innerHTML);");
				}
				if(arg0 == 1)
				{
					if(mTts.isSpeaking())
					{
						isPause = false;
						btn_state.setImageResource(R.drawable.start);
						btntate = 0;
						mTts.stopSpeaking();
						speechProgressBar.setProgress(0);
						speechProgressBar.setVisibility(View.GONE);
					}
					
					String url = webViewMain.getUrl();
					if(url == null)
						webViewMain.loadUrl("https://m.baidu.com");
					
				}
			}
			
			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {}
			
			@Override
			public void onPageScrollStateChanged(int arg0) {}
		});
		
//		title = (TextView) findViewById(R.id.title);
		tv_info = (TextView) view2.findViewById(R.id.info);
		webViewMain = (SuperWebView) view1.findViewById(R.id.webview);
		tv_head = (TextView) findViewById(R.id.tv_head);
		btn_left = (ImageButton) findViewById(R.id.btn_left);
		btn_right = (ImageButton) findViewById(R.id.btn_right);
		btn_state = (ImageButton) findViewById(R.id.btn_state);
		btn_menu = (ImageButton) findViewById(R.id.btn_menu);
		btn_microphone = (ImageButton) findViewById(R.id.btn_microphone);
		speechProgressBar = (NumberProgressBar) findViewById(R.id.speechProgressBar);
		rl_head = (RelativeLayout) findViewById(R.id.rl_head);
		webviewProgressBar = (NumberProgressBar) view1.findViewById(R.id.webviewprogressbar);
		
		btn_left.setOnClickListener(this);
		btn_right.setOnClickListener(this);
		btn_microphone.setOnClickListener(this);
		btn_state.setOnClickListener(this);
		btn_menu.setOnClickListener(this);
		tv_head.setOnClickListener(this);
		
		popWindow = new MenuPopupWindow(MainActivity.this,MainActivity.this,getWindow(),new OnDismissListener(){
			@Override
			public void onDismiss() {
				// TODO Auto-generated method stub
				btn_menu.setImageResource(R.drawable.menu);
			}
		});
		
		SuperWebView webViewHome = (SuperWebView) view0.findViewById(R.id.webview);
		webViewMain.setCutWebViewCallback(this);
//		webViewHome.setCutWebViewCallback(this);
		webViewMain.setReceiveHtmlListener(this);
		webViewHome.setReceiveMessageListener(this);
		
        webViewHome.loadUrl("file:///android_asset/welcomepage/index.html");
        if(saving)
        	webViewMain.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        else
        	webViewMain.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
	}
	private void initSpeechUtil(){
		SpeechUtility.createUtility(getApplicationContext(), SpeechConstant.APPID +"=57163d34"); //568fba83   

		mIatDialog = new RecognizerDialog(MainActivity.this, mInitListener);
		mIatDialog.setListener(mRecognizerDialogListener);
		
		mTts= SpeechSynthesizer.createSynthesizer(this, null);  
		mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoqi");
		mTts.setParameter(SpeechConstant.SPEED, "50");
		mTts.setParameter(SpeechConstant.VOLUME, "50");
		mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_AUTO); //�����ƶ�  
	}
	private InitListener mInitListener = new InitListener() {
		@Override
		public void onInit(int code) {
		}
	};
	
	private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
		public void onResult(RecognizerResult results, boolean isLast) {
			isPause = false;
			btn_state.setImageResource(R.drawable.start);
			btntate = 0;
			List<String> list= JsonParser.parseIatResult(results.getResultString());

			long current = System.currentTimeMillis();
			if(list.get(0).equals("��") || list.get(0).equals(""))
				return ;
			if(list.get(list.size() -1).equals("��"))
				list.remove(list.size()-1);
			
			if(current - lastTime > 800)
			{
				sp.play(musicEnd, 1, 1, 0, 0, 1);
				tv_info.setText("");
				speechProgressBar.setVisibility(View.GONE);
				htmlBean.content = "";
				
//				int cmdType = ParseCommand.prase(list);
				lastTime = current;
				
				handlerCMD(list);
//				if(cmdType != ParseCommand.Cmd_NewsNum)
//					cmdList.add(cmdType);
//				
//				switch (cmdType) {
//				case ParseCommand.Cmd_Search:
//					cmdSearch(list);
//					break;
//				case ParseCommand.Cmd_News:
//					cmdReadNews();
//					
//					break;
//				case ParseCommand.Cmd_Weather:
//					BaseAppLocation baseAppLocation = BaseAppLocation.getInstance();
//					BDLocation location  = baseAppLocation.getLocation();
//					String url = null;
//					if(location != null)
//					{
//						String cityname = location.getCity().replace("��", "");
//						cityname = Trans2PinYin.trans2PinYin(cityname);
//						url = "http://weather1.sina.cn/?code="+cityname+"&vt=4";
//					}else
//						url = "http://weather1.sina.cn/?vt=4";
//						
//					webView.loadUrl(url);
//					break;
//				
//				case ParseCommand.Cmd_NewsNum:
//					int pageType = ParsePageType.getPageType(htmlBean.url);
//					if( pageType== ParsePageType.MailListTag || pageType == ParsePageType.MailContentTag)
//					{
//						//������ʼ�����
//						if(mailList != null && mailList.size()>0)
//						{
//							cmdType = ParseCommand.Cmd_Mail_MailContent;
//							readMailContent(ParseCommand.praseNewsIndex(list));
//						}else
//							ToastUtil.toast("��ȡ�ʼ�����ʧ�ܣ����Ժ�����");
////							mTts.startSpeaking("��ȡ�ʼ�����ʧ�ܣ����Ժ�����",mSynListener);
//						break;
//					}
//					
//					if(pageType == ParsePageType.NewsListTag || pageType == ParsePageType.NewsContentTag)
//					{
//						//�������������
//						if(newsList != null && newsList.size() > 0)
//						{	
//							readNewsContent(ParseCommand.praseNewsIndex(list));
//						}else
//						{
//							ToastUtil.toast("��ȡ��������ʧ�ܣ����Ժ�����");
////							mTts.startSpeaking("��ȡ��������ʧ�ܣ����Ժ�����",mSynListener);
//						}
//						
//						break;
//					}
//					
//					if(cmdList.size() >0 && cmdList.get(cmdList.size() -1) == ParseCommand.Cmd_Query_Bookmark)
//					{
//						//������ǩ����
////						if(mailList != null && mailList.size()>0)
////						{
////							browserState = ParseCommand.Cmd_Original;
//							openUrlFromBookmark(ParseCommand.praseNewsIndex(list));
////						}else
////							mTts.startSpeaking("����ҳʧ�ܣ����Ժ�����",mSynListener);
//						break;
//					}
//					
//					mTts.startSpeaking("ָ�������������ȷָ��",mSynListener);
//					break;
//				case ParseCommand.Cmd_Location:
//					webView.loadUrl("http://map.qq.com/m/index/map");
//					break;
//				case ParseCommand.Cmd_Exit:
//					mTts.startSpeaking("���ڹرջ����ˡ�����", mSynListener);
//					handler.sendEmptyMessageDelayed(0, 3000);
//					break;
//				case ParseCommand.Cmd_Mail:
//					cmdMail();
//					break;
//				case ParseCommand.Cmd_Query_Bookmark:
//					cmdQueryBookmark();
//					break;
//				case ParseCommand.Cmd_Add_Bookmark:
//					cmdAddBookmark();
//					break;
//				case ParseCommand.Cmd_Err:
//				case ParseCommand.Cmd_Other:
//				default:
//					ToastUtil.toast("ָ�������������ȷָ��");
////					mTts.startSpeaking("ָ�������������ȷָ��",mSynListener);
//					break;
//				}
			}
		}
		/**
		 * ʶ��ص�����.
		 */
		public void onError(SpeechError error) {
			//showTip(error.getPlainDescription(true));
		}
	};
	
	
	private void handlerCMD(List<String> list) {
		int cmdType = ParseCommand.prase(list);
		
		if(cmdType != ParseCommand.Cmd_NewsNum)
			cmdList.add(cmdType);
		String str = "";
		for(String temp:list)
		{
			str += temp;
		}
		str = str.replace("����", "");
		
		switch (cmdType) {
		case ParseCommand.Cmd_Search:
			cmdSearch(str);
			break;
		case ParseCommand.Cmd_News:
			mailList = null;
			currentIndex = -1;
			cmdReadNews(ParseFengNews.HOME_URL);
			break;
		case ParseCommand.Cmd_Weather:
			cmdWeather();
			break;
		
		case ParseCommand.Cmd_NewsNum:
			int index = ParseCommand.praseNewsIndex(list);
			currentIndex = index;
			if(cmdList.size() >0 && cmdList.get(cmdList.size() -1) == ParseCommand.Cmd_Query_Bookmark)
			{
				//������ǩ����
				if(bookmarkList != null && bookmarkList.size() > 0)
					openUrlFromBookmark(index);
				break;
			}
			
			int pageType = ParsePageType.getPageType(htmlBean.url);
			if( pageType== ParsePageType.MailListTag || pageType == ParsePageType.MailContentTag)
			{
				//������ʼ�����
				if(mailList != null && mailList.size()>0)
				{
					cmdType = ParseCommand.Cmd_Mail_MailContent;
					readMailContent(index);
				}else
					ToastUtil.toast("��ȡ�ʼ�����ʧ�ܣ����Ժ�����");
//					mTts.startSpeaking("��ȡ�ʼ�����ʧ�ܣ����Ժ�����",mSynListener);
				break;
			}
			
			if(pageType == ParsePageType.NewsListTag || pageType == ParsePageType.NewsContentTag || pageType == ParsePageType.FengNewsContentTag || pageType == ParsePageType.FengNewsTag)
			{
				//�������������
				if(newsList != null && newsList.size() > 0)
				{	
					readNewsContent(index);
				}else
				{
					ToastUtil.toast("��ȡ��������ʧ�ܣ����Ժ�����");
//					mTts.startSpeaking("��ȡ��������ʧ�ܣ����Ժ�����",mSynListener);
				}
				break;
			}
			mTts.startSpeaking("ָ�������������ȷָ��",mSynListener);
			break;
		case ParseCommand.Cmd_Next:
			//����һ������
			if(newsList != null)
				if(currentIndex > 0 && newsList.size() >= ++currentIndex)
				{//������
					readNewsContent(currentIndex);
					break;
				}
			//����һ���ʼ�
			if(mailList != null)
				if(currentIndex > 0 && mailList.size() >= ++currentIndex)
				{
					readMailContent(currentIndex);
					break;
				}
			mTts.startSpeaking("ָ�����", mSynListener);
			break;
		case ParseCommand.Cmd_Location:
			cmdLocation();
			break;
		case ParseCommand.Cmd_Exit:
			mTts.startSpeaking("���ڹرջ����ˡ�����", mSynListener);
			handler.sendEmptyMessageDelayed(0, 3000);
			break;
		case ParseCommand.Cmd_Mail:
			newsList = null;
			currentIndex = -1;
			cmdMail();
			break;
		case ParseCommand.Cmd_Query_Bookmark:
			cmdQueryBookmark();
			break;
		case ParseCommand.Cmd_Add_Bookmark:
			cmdAddBookmark();
			break;
		case ParseCommand.Cmd_Err:
		case ParseCommand.Cmd_Other:
		default:
//			ToastUtil.toast("ָ�������������ȷָ��");
			String url1 = "http://m.baidu.com/s?word="+str;
			webViewMain.loadUrl(url1);
//			mViewPager.setCurrentItem(1);

//			mTts.startSpeaking("ָ�������������ȷָ��",mSynListener);
			break;
		}
	}
	private void cmdLocation() {
		// TODO Auto-generated method stub
		webViewMain.loadUrl("http://map.qq.com/m/index/map");
//		mViewPager.setCurrentItem(1);

	}
	private void cmdWeather() {
		BaseAppLocation baseAppLocation = BaseAppLocation.getInstance();
		BDLocation location  = baseAppLocation.getLocation();
		String url = null;
		if(location != null)
		{
			try{
				
			String cityname = location.getCity().replace("��", "");
			cityname = Trans2PinYin.trans2PinYin(cityname);
			url = "http://weather1.sina.cn/?code="+cityname+"&vt=4";
			}catch(NullPointerException e){
				url = "http://weather1.sina.cn/?vt=4";
			}
		}else
			url = "http://weather1.sina.cn/?vt=4";
			
		webViewMain.loadUrl(url);
//		mViewPager.setCurrentItem(1);

	}
	
	private void cmdQueryBookmark() {
		// TODO Auto-generated method stub
		MyDataBase db = MyDataBase.getInstance();
		bookmarkList = db.queryBookMark();
		if(bookmarkList.size() == 0)
		{
			ToastUtil.toast("����δ�����ǩ...");

			mTts.startSpeaking("����δ�����ǩ...", mSynListener);
		}
		else
		{
			int count = bookmarkList.size();
			String str = "������ǩ����" + count+"����\n";
			for(int i = 1;i <= count;i++)
			{
				str+="��"+i+"��:"+bookmarkList.get(i - 1).title+"\n";
			}
			tv_info.setText(str);
			htmlBean.content = str;
			
			mTts.startSpeaking(str, mSynListener);
		}
	}
	
	protected void openUrlFromBookmark(int praseNewsIndex) {
		// TODO Auto-generated method stub
		MyDataBase db = MyDataBase.getInstance();
		List<BookMarkBean> list = db.queryBookMark();
		if(praseNewsIndex > list.size())
		{
			ToastUtil.toast("��ǩ������");
//			mTts.startSpeaking("��ǩ������", mSynListener);
		}
		else
		{
			String url = list.get(praseNewsIndex - 1).url;
			String title = list.get(praseNewsIndex - 1).title;
			webViewMain.loadUrl(url);
//			mTts.startSpeaking("����Ϊ����"+title+"�����Ժ�", mSynListener);
		}
	}

	protected void cmdAddBookmark(){
		// TODO Auto-generated method stub
		MyDataBase db = MyDataBase.getInstance();
		BookMarkBean bean = new BookMarkBean();
		bean.url = htmlBean.url;
		bean.title = Jsoup.parse(htmlBean.html).title();
		if(db.insertBookMark(bean) != -1)
		{
//			mTts.startSpeaking("�ѳɹ���"+bean.title+"��ӵ���ǩ", mSynListener);
			ToastUtil.toast("�ѳɹ���"+bean.title+"��ӵ���ǩ");

		}else
		{
			ToastUtil.toast("��ǩ���ʧ��");
//			mTts.startSpeaking("��ǩ���ʧ��", mSynListener);
		}
	}

	private void cmdMail()
	{
		webViewMain.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36");
		webViewMain.loadUrl("https://ui.ptlogin2.qq.com/cgi-bin/login?style=9&appid=522005705&daid=4&s_url=https%3A%2F%2Fw.mail.qq.com%2Fcgi-bin%2Flogin%3Fvt%3Dpassport%26vm%3Dwsk%26delegate_url%3D%26f%3Dxhtml%26target%3D&hln_css=http%3A%2F%2Fmail.qq.com%2Fzh_CN%2Fhtmledition%2Fimages%2Flogo%2Fqqmail%2Fqqmail_logo_default_200h.png&low_login=1&hln_autologin=%E8%AE%B0%E4%BD%8F%E7%99%BB%E5%BD%95%E7%8A%B6%E6%80%81&pt_no_onekey=1");
//		mViewPager.setCurrentItem(1);

	}
	
	protected void readMailContent(int praseNewsIndex) {
		// TODO Auto-generated method stub
		if(praseNewsIndex > mailList.size())
		{
//			mTts.startSpeaking("�����������ڣ�����������ָ��", mSynListener);
			ToastUtil.toast("�����������ڣ�����������ָ��");
		}
		else
			webViewMain.loadUrl(mailList.get(praseNewsIndex - 1).mailUrl);
	}

	private void readNewsContent(final int praseNewsIndex) {
		// TODO Auto-generated method stub
		if(praseNewsIndex > newsList.size())
		{
//			mTts.startSpeaking("�����������ڣ�����������ָ��", mSynListener);
			ToastUtil.toast("�����������ڣ�����������ָ��");
		}
		else
			webViewMain.loadUrl(newsList.get(praseNewsIndex - 1).newsUrl);
	}
	@Override
	public void onShake() {
		// TODO Auto-generated method stub
//		mBDTts.stop();
		if(mTts.isSpeaking())
			mTts.stopSpeaking();
		if(System.currentTimeMillis() - lastShakeTime > 1200)
		{	
			mVibrator.vibrate(500);
			sp.play(musicStart, 1, 1, 0, 0, 1);
			mIatDialog.show();
		}
		lastShakeTime = System.currentTimeMillis();
	}
	
	
	private SynthesizerListener mSynListener = new SynthesizerListener()
	{  
	    //�Ự�����ص��ӿڣ�û�д���ʱ��errorΪnull  
	    public void onCompleted(SpeechError error) {
//	    	btn_stop.setText("��ʼ����");
	    	speechProgressBar.setVisibility(View.GONE);
	    	btn_state.setImageResource(R.drawable.start);
			btntate = 0;
	    }  
	    //������Ȼص�  
	    //percentΪ�������0~100��beginPosΪ������Ƶ���ı��п�ʼλ�ã�endPos��ʾ������Ƶ���ı��н���λ�ã�infoΪ������Ϣ��  
	    public void onBufferProgress(int percent, int beginPos, int endPos, String info) {}  
	    //��ʼ����  
	    public void onSpeakBegin() {
	    	//Toast.makeText(getApplicationContext(), "Begin", Toast.LENGTH_SHORT).show();
//	    	btn_stop.setText("ֹͣ����");
	    	speechProgressBar.setVisibility(View.VISIBLE);
	    	speechProgressBar.setMax(100);
	    	speechProgressBar.setProgress(0);
	    	btn_state.setImageResource(R.drawable.pause);
			btntate = 1;
	    }  
	    
	    
	    //��ͣ����  
	    public void onSpeakPaused() {
//	    	btn_state.setImageResource(R.drawable.start);
//			btntate = 0;
	    }  
	    //���Ž��Ȼص�  
	    //percentΪ���Ž���0~100,beginPosΪ������Ƶ���ı��п�ʼλ�ã�endPos��ʾ������Ƶ���ı��н���λ��.  
	    public void onSpeakProgress(int percent, int beginPos, int endPos) {
	    	speechProgressBar.setProgress(percent);
	    }  
	    //�ָ����Żص��ӿ�  
	    public void onSpeakResumed() {
//	    	btn_state.setImageResource(R.drawable.pause);
//			btntate = 1;
	    }  
	//�Ự�¼��ص��ӿ�  
	    public void onEvent(int arg0, int arg1, int arg2, Bundle arg3) {
//	    	if(arg0 == SpeechEvent.Event)
//	    		btn_stop.setText("��ʼ����");
	    	
	    }  
	};

	
	private void cmdReadNews(String url){
		webViewMain.loadUrl(url);

//		webViewMain.loadUrl(ParseTencentNews.HOMEURL);
//		mViewPager.setCurrentItem(1);
	}
	
	private void cmdSearch(String str) {
//		String str = "";
//		for(String temp:list)
//		{
//			str += temp;
//		}
//		str = str.replace("����", "");
		String url = "http://m.baidu.com/s?word="+str;
		webViewMain.loadUrl(url);
//		mViewPager.setCurrentItem(1);

	}
	
	Handler handler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 0:
				exitApp();
				break;
			case 1:
//				List<String> resList = (List<String>) msg.obj;
				break;
			}
		}
	};
	@Override
	protected void onResume() {
		if(shake)
			Shake.registerListener(this, this);
		super.onResume();
	}
	@Override
	protected void onPause() {
		if(shake)
			Shake.removeListener();
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		webViewMain.destroy();
		exitApp();
	}
	
	private void exitApp()
	{
		if(mTts.isSpeaking())
			mTts.stopSpeaking();
		mTts.destroy();
		webViewMain.destroy();
		finish();
		BaseAppLocation baseAppLocation = BaseAppLocation.getInstance();
		baseAppLocation.removeLocationListener();
		System.exit(0);
		android.os.Process.killProcess(android.os.Process.myPid());
	}
	 
	 	@Override
	    public void onBackPressed() {
	 		if(mViewPager.getCurrentItem() == 2 || mViewPager.getCurrentItem() == 0)
	 		{
	 			mViewPager.setCurrentItem(1);
	 			return;
	 		}
	 		
	 		if(mTts.isSpeaking())
				mTts.stopSpeaking();
	        if(webViewMain.canGoBack())
	            webViewMain.goBack();
	        else
	        {
	        	AlertDialog.Builder builder = new Builder(this);
	        	builder.setMessage("ȷ���˳���");  
	        	builder.setTitle("�������˳����������");
	        	mTts.startSpeaking("�������˳�������������밴ȷ�����˳���", mSynListener);
	        	builder.setPositiveButton("��ȷ��", new DialogInterface.OnClickListener(){

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dialog.dismiss();
						exitApp();
					}
	        	});
	        	
	        	builder.setNegativeButton("������", new DialogInterface.OnClickListener(){

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dialog.dismiss();
					}
	        	});
	        	builder.create().show();
	        }
	    }
//	    public static void writeFileSdcard(String fileName,String message)
//	    { 
//	    	try {  
//	    	    File file = new File(Environment.getExternalStorageDirectory(),  
//	    	            "c.txt");  
//	    	        FileOutputStream fos = new FileOutputStream(file, false);  
//	    	 
//	    	           fos.write(message.getBytes("utf-8"));  
//	    	           fos.close();  
//	    	           //Toast.makeText(getApplicationContext(), "д��ɹ�", Toast.LENGTH_SHORT).show();
//	    	} catch (Exception e) {
//	    	    e.printStackTrace();  
//	    	}  
//	    }
	    
		@Override
		public void onReceiveHTML(String url,String html) {
			// TODO Auto-geerated method stub
//			tv_info.setText("");
			int tag = ParsePageType.getPageType(url);
			htmlBean.url = url;
			if(html != null && html.length() > 0)
			{
				
			}else
				return;
			htmlBean.html = html;
			String title = Jsoup.parse(html).title();
//			tv_head.setText(title);
			btn_state.setImageResource(R.drawable.start);
			isPause = false;
			btntate = 0;
			if(url != null && url.length() >0 && title != null && title.length() > 0)
			{//������ʷ��¼
				MyDataBase myDataBase = MyDataBase.getInstance();
				HistoryBean bean = new HistoryBean();
				bean.time = System.currentTimeMillis()+"";
				bean.url = url;
				bean.title = title;
				myDataBase.insertHistory(bean);
			}
			btntate = 0;
			switch (tag) {
//			case ParsePageType.MailLoginTag:
//				processLoginQQMail();
//				break;
//			case ParsePageType.MailHomePageTag:
//				processQQMailHome();
//				break;
			case ParsePageType.MailListTag:
				processMailList();
				break;
			case ParsePageType.MailContentTag:
				processMailContent();
				break;
			case ParsePageType.SinaWeatherTag:
				processSinaWeather();
				break;
			case ParsePageType.BaiduResultUrlTag:
				processSearchResult();
				break;
			case ParsePageType.NewsListTag:
				processNewsList();
				break;
			case ParsePageType.NewsContentTag:
				processNewsContent();
				break;
			case ParsePageType.TencentMapUrlTag:
				processGetLocation();
				break;
			case ParsePageType.FengNewsTag:
				processFengNewsList();
				break;
			case ParsePageType.FengNewsContentTag:
				processFengNewsContent();
				break;
			default:
				if(url != null && url.contains("m.baidu.com") && url.length() < 21)
				{
					htmlBean.content = "��ǰ��ҳ�ݲ�֧�ֽ���";
					break;
				}

				try {
					String content = Extractor.getContentByHtml(html);
					htmlBean.content = content;
				} catch (Exception e) {
					// TODO Auto-generated catch block
//					ToastUtil.toast("����ʧ��");
				}
				break;
			}
			tv_info.setText(htmlBean.content);
			if(autoread || blind)
				if(!htmlBean.content.contains("��ǰ��ҳ�ݲ�֧�ֽ���"))
				mTts.startSpeaking(htmlBean.content, mSynListener);
		}


		private void processFengNewsContent() {
	// TODO Auto-generated method stub
			htmlBean.content = "";
			News news = ParseFengNews.ParseFengNewsContent(htmlBean.html);
			if(news == null)
			{
				htmlBean.content = "���������ȡʧ��...";
			}else {
//				htmlBean.content = "���⣺"+news.getTitle()+"\n";
				htmlBean.content += news.getContent();
			}
		}
		@Override
		public void onShouldOverrideUrl(String url) {
			// TODO Auto-generated method stub
			if(mTts.isSpeaking())
			{
				mTts.stopSpeaking();
				isPause = false;
				btntate = 0;
			}
			htmlBean.content = "";
			tv_info.setText("����Ϊ��Ŭ������...");
			htmlBean.url = url;
			speechProgressBar.setVisibility(View.GONE);
//			mViewPager.setCurrentItem(1);

		}

		@Override
		public void onClick(View v)
		{
			// TODO Auto-generated method stub
			switch (v.getId()) 
			{
			case R.id.btn_m_bookmark:
				processBookmark();
				break;
			case R.id.btn_m_email:
				Intent intent = new Intent();
				intent = new Intent(MainActivity.this,MailManagerActivity.class);
				startActivity(intent);
				break;
			case R.id.btn_m_setting:
				processSetting();
				break;
			case R.id.btn_m_exit:
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						exitApp();
					}
				}, 100);
				break;
			case R.id.btn_menu:
				popWindow.showPopupWindow(findViewById(R.id.toolsBar));
				btn_menu.setImageResource(R.drawable.down);
				break;
			case R.id.btn_left:
				if(mTts.isSpeaking())
					mTts.stopSpeaking();
		        if(webViewMain.canGoBack())
		            webViewMain.goBack();
		        else
		        {
		        	ToastUtil.toast("�Ѿ��ǵ�һҳ��");
		        }
				break;
			case R.id.btn_right:
				if(mTts.isSpeaking())
					mTts.stopSpeaking();
				if(webViewMain.canGoForward())
					webViewMain.goForward();
				else
				{
					ToastUtil.toast("�Ѿ������һҳ��");
				}
				break;
			case R.id.btn_state:
				switch (btntate) {
				case 0://��ʼ
					if(isPause)
					{
						mTts.resumeSpeaking();
						isPause = false;
						btn_state.setImageResource(R.drawable.pause);
						btntate = 1;
					}else
					{
						switch (mViewPager.getCurrentItem()) {
						case 0:
							mTts.startSpeaking("��ѡ��������ʵ���ҳ��Ȼ����ܲ�������", mSynListener);
							break;
						case 1:
							mViewPager.setCurrentItem(2);

						case 2:
							mTts.startSpeaking(htmlBean.content, mSynListener);
						default:
							break;
						}
					}
					break;
				case 1://��ͣ
					mTts.pauseSpeaking();
					isPause = true;
					btn_state.setImageResource(R.drawable.start);
					btntate = 0;
					break;
				case 2://ֹͣ
					if(!mTts.isSpeaking())
						mTts.stopSpeaking();
					btn_state.setImageResource(R.drawable.start);
					btntate = 0;
					break;
				default:
					break;
				}
						
				break;
			case R.id.btn_microphone:
				onShake();
				break;
			case R.id.btn_m_homepage:
				htmlBean.content = "";
				mViewPager.setCurrentItem(0);
//				webView.loadUrl("http://m.baidu.com");
				break;
			case R.id.btn_m_history:
				intent = new Intent(MainActivity.this,HistoryActivity.class);
				startActivityForResult(intent, REQUEST_CODE_HISTORY);
				break;
			case R.id.btn_m_other:
				intent = new Intent(MainActivity.this,LogActivity.class);
				startActivity(intent);
				break;
			case R.id.btn_m_refresh:
				htmlBean.content = "";
				webViewMain.reload();
				break;
			case R.id.tv_head:
//				et_head.setText(htmlBean.url);
				EditUrlPopupWindow urlPopupWindow = new EditUrlPopupWindow(this, new EditUrlPopupDismissListener() {
					@Override
					public void onDismiss(int type,String content) {
						// TODO Auto-generated method stub
//						mViewPager.setCurrentItem(1);
						switch (type) {
						case EditUrlPopupWindow.TYPE_URL:
							webViewMain.loadUrl(content);
//							mViewPager.setCurrentItem(1);
							break;
						case EditUrlPopupWindow.TYPE_CNT:
							List<String> list = new ArrayList<String>();
							list.add(content);
							handlerCMD(list);
						default:
							break;
						}
					}
				});
				urlPopupWindow.show(rl_head, htmlBean.url);
				break;
			default:
				break;
			}
			
			if(v.getId() != R.id.btn_menu)
			{
				popWindow.dismiss();
			}
		}
		
		
		private void processFengNewsList() {
			// TODO Auto-generated method stub
			List<News> newsList = ParseFengNews.parseFengNewsList(htmlBean.html);
			if(newsList != null && newsList.size() > 0)
			{
				htmlBean.content = "";
				List<NewsBean> temp = new ArrayList<NewsBean>();
				for(int i = 1;i <= newsList.size();i++)
				{
					News news = newsList.get(i-1);
					htmlBean.content += "��"+i+"����"+news.getTitle()+"\n";
//					htmlBean.content += news.getUrl()+"\n\n";
					NewsBean bean = new NewsBean();
					bean.newsTitle = news.getTitle();
					bean.newsUrl = news.getUrl();
					temp.add(bean);
				}
				this.newsList = temp;
			}else
			{
//				htmlBean.content = "�����б��ȡʧ��";

				htmlBean.content = "";
			}
		}
		
		public void processGetLocation()
		{
			BaseAppLocation baseAppLocation = BaseAppLocation.getInstance();
			BDLocation location  = baseAppLocation.getLocation();
			if(location != null && location.getAddrStr() != null && location.getAddrStr().length() > 0){
				String content = "����ǰλ�ڣ�"+location.getAddrStr();
//				mTts.startSpeaking(content, mSynListener);
				htmlBean.content = content;
			}else
			{
				String content = "��δ��ȡ������λ�ã������Ƿ�������λ��Ȩ��.";
//				mTts.startSpeaking(content, mSynListener);
				htmlBean.content = content;
			}
		}
		
		public void processLoginQQMail()
		{
			MyDataBase db = MyDataBase.getInstance();
			List<MailBean> mailList = db.queryMail("QQ");
			if(mailList != null && mailList.size() > 0)
			{
				MailBean bean = mailList.get(mailList.size() - 1);
				ToastUtil.toast("����Ϊ����½"+bean.type+"����...");
				webViewMain.loadUrl("javascript:"
						+ "document.getElementById(\"u\").value= \"" + bean.username + "\";"
						+ "document.getElementById(\"p\").value= \"" + bean.password + "\";"
						+ "document.getElementById(\"go\").click();");
			}else
			{
				ToastUtil.toast("����������QQ�����˺ţ��Ա�ʹ���ʼ�����...");
				mTts.startSpeaking("����������QQ�����˺ţ��Ա�ʹ���ʼ�����...", mSynListener);
			}
		}
		
		public void processQQMailHome()
		{
			mailCookie = webViewMain.getCookie();
			int cookieStart = mailCookie.indexOf("msid=") + 5;
			int cookieEnd = mailCookie.indexOf(";", cookieStart);
			if(cookieStart != -1 && cookieEnd != -1 && cookieEnd > cookieStart)
			{
				msid = mailCookie.substring(cookieStart,cookieEnd);
				
//				String html = htmlBean.html;
				String html = htmlBean.url;

				int start = html.indexOf("/cgi-bin/today");
				//�ж��Ƿ�������������
				if(start != -1)
				{
//					browserState = ParseCommand.Cmd_Mail_InBox;
//					int end = html.indexOf(">", start);
//					if(end != -1)
//					{
						webViewMain.loadUrl("https://w.mail.qq.com/cgi-bin/mail_list?fromsidebar=1&sid="+msid+"&folderid=1&page=0&pagesize=10&sorttype=time&t=mail_list&loc=today,,,151&version=html");	
						return;
//					}else
//					{
//						String str = "�����½ʧ�ܣ����Ժ�����";
//						ToastUtil.toast(str);
//						htmlBean.content = str;
////						mTts.startSpeaking(str, mSynListener);
//					}
				}
			}else
			{
				String str = "�����½ʧ�ܣ����Ժ�����";
				ToastUtil.toast(str);
				htmlBean.content = str;
//				mTts.startSpeaking("�����½ʧ�ܣ����Ժ�����", mSynListener);
			}
		}
		public void processMailContent()
		{
			if(htmlBean.html.length() > 0)
			{
				String mailContent = ParseMailContent.praseMailContent(htmlBean.html);
				htmlBean.content = mailContent;
//				mTts.startSpeaking(htmlBean.content, mSynListener);
			}
			else
			{
				String str = "�ʼ������ȡʧ�ܣ����Ժ�����";
				ToastUtil.toast(str);
				htmlBean.content = str;
//				mTts.startSpeaking("�ʼ������ȡʧ�ܣ����Ժ�����", mSynListener);
			}
		}
		
		public void processMailList()
		{
			String html = htmlBean.html;
			currentIndex = -1;
//			browserState = ParseCommand.Cmd_Original;
			List<MailListBean> list = ParseMailList.parseMailList(html);
			if(list.size() == 0)
			{
//				String str = "�ʼ���ȡʧ�ܣ����Ժ�����";
//				ToastUtil.toast(str);
//				htmlBean.content = str;
//				mTts.startSpeaking("��ȡʧ�ܣ����Ժ�����", mSynListener);
			}
			else
			{
				mailList = list;
				String speakStr;
				if(list.size() > 0)
				{
					speakStr = "���������"+list.size()+"���ʼ���\n";
					int i = 1;
					for(i = 1;i <= list.size();i++)
					{
						speakStr += "��"+i+"��������"+list.get(i-1).mailFrom+"�����⣺"+list.get(i-1).mailTitle+"\n";
					}
				}
				else
				{
						speakStr = "�����ռ������������ʼ�";
						ToastUtil.toast(speakStr);
				}
				htmlBean.content = speakStr;
				
//				if(blind)
//					mTts.startSpeaking(htmlBean.content, mSynListener);
			}
			
//			Log.e("MAILList", list.size()+"");
		}
		
//		public void processWeather()
//		{
//			String html = htmlBean.html;
//			int end = 0;
//			int start = 0;
//			if(html.length() < 1)
//			{
//				Toast.makeText(getApplicationContext(), "δ��ȡ������������λ��Ȩ��", Toast.LENGTH_SHORT).show();
//				return;
//			}
//			List<WeatherBean> weatherList = new ArrayList<WeatherBean>();
////			weatherList = PraseWeatherHtml.praseWeatherList(html);
//			
//			String title = Jsoup.parse(html).title();
//			end = -1;
//			end = title.indexOf(' ');
//			if(end != -1)
//			{
//				title = title.substring(0, end);
//				title = "����Ϊ������" + title+":\n";
//			}
//			else
//				title= "����Ϊ������δ����������״��:";
//			
//			String str = "";
//			for(WeatherBean bean:weatherList)
//			{
//				str += bean.date+"��"+bean.weather+'��'+bean.temp+"��\n";
//			}
//			
//			htmlBean.content = title + str;
//			mTts.startSpeaking(htmlBean.content,mSynListener);
//		}
		
		public void processNewsList()
		{
			String html = htmlBean.html;
			newsList = ParseTencentNews.getNewsList(html);
			String titleStr = "";
			for(int i = 1;i <= 100 && i <= newsList.size();i++)
			{
				titleStr += "��"+i+"����"+newsList.get(i-1).newsTitle+"\n";
			}
			
			htmlBean.content = titleStr;
//			mTts.startSpeaking(htmlBean.content, mSynListener);
		}
		
		public void processNewsContent()
		{
			String html = htmlBean.html;
			String content = ParseTencentNews.getNewsContent(html);
			String title = Jsoup.parse(htmlBean.html).title();
			title = title.replace("-�ֻ���Ѷ��", "");
			htmlBean.content = "���⣺" + title+"\n"+content;
//			mTts.startSpeaking(htmlBean.content, mSynListener);
		}
		
		public void processSearchResult()
		{
			String html = htmlBean.html;
			String searchResult = "";
			List<String> resList = BaiduSearch.praseSearchResultList(html);
			if(resList.size() != 0)
			{
				for(int i = 1;i <= resList.size();i++)
					searchResult += "��"+i+"����"+resList.get(i-1)+"\n";
				htmlBean.content = "�������������:\n"+searchResult;
//				mTts.startSpeaking(htmlBean.content, mSynListener);
			}else
			{	
				ToastUtil.toast("�������������ԡ�");
//				mTts.startSpeaking("�������������ԡ�", mSynListener);
			}
		}
		
		public void processSinaWeather()
		{
			htmlBean.content = ParseWeatherHtml.praseWeatherList(htmlBean.html);
//			mTts.startSpeaking(htmlBean.content,mSynListener);
		}
		
		public void processBookmark(){
			Intent intent = new Intent(MainActivity.this,BookMarkActivity.class);
			intent.putExtra("url", htmlBean.url);
			intent.putExtra("title", Jsoup.parse(htmlBean.html).title());
			startActivityForResult(intent, REQUEST_CODE_BOOKMARK);
		}
		
		public void processSetting(){
			Intent intent = new Intent();
			intent = new Intent(MainActivity.this,SettingActivity.class);
			startActivityForResult(intent,REQUEST_CODE_SETTING);
		}
		
		@Override
		protected void onActivityResult(int requestCode, int resultCode, Intent data) {
			// TODO Auto-generated method stub
			switch(requestCode)
			{
			case REQUEST_CODE_BOOKMARK:
				if(resultCode == RESULT_OK)
				{
					String url = data.getStringExtra("url");
					webViewMain.getSettings().setUserAgentString("Mozilla/5.0 (Linux; U; Android 5.1.1; zh-cn; PLK-UL00 Build/HONORPLK-UL00) AppleWebKit/537.36 (KHTML, like Gecko)Version/4.0 MQQBrowser/5.3 Mobile Safari/537.36");
					webViewMain.loadUrl(url);
				}
				break;
			case REQUEST_CODE_HISTORY:
				if(resultCode == RESULT_OK)
				{
					String url = data.getStringExtra("url");
					webViewMain.getSettings().setUserAgentString("Mozilla/5.0 (Linux; U; Android 5.1.1; zh-cn; PLK-UL00 Build/HONORPLK-UL00) AppleWebKit/537.36 (KHTML, like Gecko)Version/4.0 MQQBrowser/5.3 Mobile Safari/537.36");
					webViewMain.loadUrl(url);
				}
				break;
			case REQUEST_CODE_SETTING:
			{
				final SharedPreferences sharedpref = MySharedPreferences.getInstance(getApplicationContext());
			    autoread = sharedpref.getBoolean("autoread", false);
			    blind = sharedpref.getBoolean("blind", false);
			    shake = sharedpref.getBoolean("shake", true);
			    saving = sharedpref.getBoolean("saving", false);
			    if(saving)
		        	webViewMain.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
		        else
		        	webViewMain.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
				break;
			}
			default:
				break;
			}
		}
		@Override
		public void onReceiveTitle(String title) {
			// TODO Auto-generated method stub
			tv_head.setText(title);
		}
		@Override
		public void onReceiveMessage(int tag) {
			// TODO Auto-generated method stub
			webViewMain.getSettings().setUserAgentString("Mozilla/5.0 (Linux; U; Android 5.1.1; zh-cn; PLK-UL00 Build/HONORPLK-UL00) AppleWebKit/537.36 (KHTML, like Gecko)Version/4.0 MQQBrowser/5.3 Mobile Safari/537.36");
			switch (tag) {
			case 6:
				webViewMain.loadUrl("http://m.baidu.com");
//				mViewPager.setCurrentItem(1);
				break;
			case 1:
//				mViewPager.setCurrentItem(1);
				cmdList.add(ParseCommand.Cmd_News);
				webViewMain.loadUrl(ParseFengNews.HOME_URL);
				break;
			case 3:
//				mViewPager.setCurrentItem(1);
				blind = false;
				cmdList.add(ParseCommand.Cmd_Mail);
				cmdMail();
				break;
			case 4:
//				mViewPager.setCurrentItem(1);
				cmdList.add(ParseCommand.Cmd_Weather);
				cmdWeather();
				break;
			case 5:
//				mViewPager.setCurrentItem(1);
				cmdList.add(ParseCommand.Cmd_Location);
				cmdLocation();
				break;
			case 2:
				cmdList.add(ParseCommand.Cmd_News);
//				mViewPager.setCurrentItem(1);
				cmdReadNews(ParseTencentNews.HOMEURL);
//				webViewMain.loadUrl("http://inews.ifeng.com/index.shtml");
				break;
			case 7:
				cmdList.add(ParseCommand.Cmd_Query_Bookmark);
				processBookmark();
				break;
			case 8:
				processSetting();
				break;
			default:
				break;
			}

		}
		
		@Override
		public void onProgressChanged(String url, int progress) {
			// TODO Auto-generated method stub
			if(progress == 100)
			{
				webviewProgressBar.setVisibility(View.GONE);
				onPageFinished(url);
			}else
			{
				webviewProgressBar.setVisibility(View.VISIBLE);
				webviewProgressBar.setProgress(progress);
			}
		}
		
		public void onPageFinished(String url) {
			// TODO Auto-generated method stub
			if (url != null && url.length() > 0)
			{
				htmlBean.url = url;
				int tag = ParsePageType.getPageType(url);
				
				switch (tag) {
				case ParsePageType.MailLoginTag:
					processLoginQQMail();
					break;
				case ParsePageType.MailHomePageTag:
					processQQMailHome();
					break;
				case ParsePageType.MailListTag:
					final SharedPreferences sharedpref = MySharedPreferences.getInstance(getApplicationContext());
				    blind = sharedpref.getBoolean("blind", false);
					if(blind)
						webViewMain.loadUrl("javascript:window.HTML.getHtml(document.getElementsByTagName('html')[0].innerHTML);");
					break;
				case ParsePageType.MailContentTag:
					if(blind)
	//					webViewMain.loadUrl("javascript:window.HTML.getHtml(document.getElementsByTagName('html')[0].innerHTML);");
						if(mViewPager.getCurrentItem() == 2)
							webViewMain.loadUrl("javascript:window.HTML.getHtml(document.getElementsByTagName('html')[0].innerHTML);");
						else {
							mViewPager.setCurrentItem(2);
						}
					break;
				case ParsePageType.NewsListTag:
					if(blind)
						handler.postDelayed(new Runnable() {
							@Override
							public void run() {
								// TODO Auto-generated method stub
								webViewMain.loadUrl("javascript:window.HTML.getHtml(document.getElementsByTagName('html')[0].innerHTML);");
							}
						}, 1000);
					break;
				case ParsePageType.FengNewsTag:
					if(blind)
					{
						if(mTts.isSpeaking())
							break;
						else
						if(mViewPager.getCurrentItem() == 2)
							webViewMain.loadUrl("javascript:window.HTML.getHtml(document.getElementsByTagName('html')[0].innerHTML);");
						else {
							mViewPager.setCurrentItem(2);
						}
					}
					break;
				default:
				{
					if(cmdList.size() >0 && cmdList.get(cmdList.size() -1) == ParseCommand.Cmd_Mail)
						break;
					
					String title = webViewMain.getTitle();
					if(title != null && title.equals("�ٶ�һ��"))
						break;
					if(blind)
					{
						if(mViewPager.getCurrentItem() == 2)
							webViewMain.loadUrl("javascript:window.HTML.getHtml(document.getElementsByTagName('html')[0].innerHTML);");
						else {
							mViewPager.setCurrentItem(2);
						}
					}
					break;
				}
				}
			}
		}
		@Override
		public void onLoadUrl(String url) {
			// TODO Auto-generated method stub
			if(url != null && url.indexOf("javascript:window") == -1)
				mViewPager.setCurrentItem(1);
			bookmarkList = null;
		}
}
