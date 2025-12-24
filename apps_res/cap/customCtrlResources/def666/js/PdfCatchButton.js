(function (_self, factory) {
	var nameSpace = 'customBtn_654321';
	if (!window[nameSpace] && typeof factory === 'function') {
		var Builder = factory();
		window[nameSpace] = { initBtn: {} };
		window[nameSpace].init = function (el, options) {
			var identification = Math.floor(Math.random() * 10000000000).toString();
			window[nameSpace].initBtn[identification] = new Builder(el, options, _self, identification);
		}
	}
})(window, function () {

	function getQueryString(name) {
		var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)", "i");
		var r = window.location.search.substr(1).match(reg);
		if (r != null) return unescape(r[2]);
		return null;
	}

	var dynamicLoading = {
		checkCss: function (name) {
			var styleList = document.getElementsByTagName('style');
			for (var i = 0; i < styleList.length; i++) {
				if (styleList[i].getAttribute('data-name') === name) return true;
			}
			return false;
		},
		insertCss: function (innerTexts, name) {
			var head = document.getElementsByTagName('head')[0];
			var style = document.createElement('style');
			style.type = 'text/css';
			style.setAttribute('data-name', name);
			try { style.appendChild(document.createTextNode(innerTexts)); }
			catch (ex) { style.styleSheet.cssText = innerTexts; }
			head.appendChild(style);
		}
	};

	function labelPrintingBtn(el, options, _self, identification) {
		var self = this;
		self.window_self = _self;
		self.el = el;
		self.identification = identification;
		self.initParams(options);
		self.initBtn(el);
		self.isLoading = false;
	}

	labelPrintingBtn.prototype = {
		initParams: function (options) {
			this.place = options.place;
			this.rowData = options.rowData;
			this.data = options.data;
			this.adaptation = options.adaptation || {};
		},

		initBtn: function (el) {
			// === CSS 样式定义 ===
			var innerTexts = ''
				+ '@keyframes gradientFlow { 0% { background-position: 0% 50%; } 50% { background-position: 100% 50%; } 100% { background-position: 0% 50%; } } '
				+ '@keyframes fadeIn { from { opacity: 0; transform: translateY(-20px); } to { opacity: 1; transform: translateY(0); } } '
				+ '.labelPrintingBtnHandler { background-color: transparent; font-family: "Ping Fang SC", "Microsoft YaHei", sans-serif; cursor: pointer; white-space: nowrap; border: 0; }'
				+ '.labelPrintingBtnBox { box-sizing: border-box; cursor: pointer; outline: none; display: inline-flex; align-items: center; justify-content: center; vertical-align: middle; margin-top: -5px; margin-right: 5px; background: linear-gradient(90deg, #d3d4d4ff, #ec8563ff, #bdc3c5ff); animation: gradientFlow 3s ease infinite; background-size: 200% 200%; color: #333; font-size: 13px; height: 30px; padding: 0 15px; border-radius: 4px; transition: all 0.3s ease; }'
				+ '.labelPrintingBtnBox:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(255, 107, 149, 0.5); color: #000; }'
				+ '.labelPrintingBtnBox i { color: #333 !important; font-size: 14px; margin-right: 5px; }'
				+ '.custom-modal-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0, 0, 0, 0.5); backdrop-filter: blur(5px); z-index: 9999; display: flex; align-items: center; justify-content: center; opacity: 0; transition: opacity 0.3s; pointer-events: none; }'
				+ '.custom-modal-overlay.active { opacity: 1; pointer-events: auto; }'
				+ '.custom-modal-box { background: #fff; width: 340px; padding: 30px; border-radius: 16px; box-shadow: 0 20px 40px rgba(0,0,0,0.2); animation: fadeIn 0.3s ease-out; font-family: "Microsoft YaHei", sans-serif; position: relative; }'
				+ '.custom-modal-title { font-size: 18px; font-weight: bold; margin-bottom: 20px; color: #333; text-align: center; }'
				+ '.custom-input-group { margin-bottom: 15px; }'
				+ '.custom-input-label { display: block; font-size: 13px; color: #666; margin-bottom: 6px; font-weight: 500; }'
				+ '.custom-form-input { width: 100%; height: 38px; padding: 0 12px; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 14px; outline: none; transition: all 0.3s; background-color: #f9f9f9; }'
				+ '.custom-form-input:focus { border-color: #ff6b95; background-color: #fff; box-shadow: 0 0 0 3px rgba(255, 107, 149, 0.1); }'
				+ '.custom-modal-btns { display: flex; justify-content: space-between; margin-top: 25px; gap: 10px; }'
				+ '.custom-btn { border: none; padding: 10px 0; flex: 1; border-radius: 8px; cursor: pointer; font-size: 14px; transition: transform 0.1s; }'
				+ '.custom-btn:active { transform: scale(0.98); }'
				+ '.custom-btn-cancel { background: #f0f2f5; color: #666; font-weight: 500; }'
				+ '.custom-btn-confirm { background: linear-gradient(135deg, #ff6b95, #ffd600); color: #fff; font-weight: bold; box-shadow: 0 4px 10px rgba(255, 107, 149, 0.3); }'
				+ '.progress-container { margin: 20px 0; }'
				+ '.progress-bar-bg { width: 100%; height: 24px; background: #f0f2f5; border-radius: 12px; overflow: hidden; position: relative; }'
				+ '.progress-bar-fill { height: 100%; background: linear-gradient(90deg, #ff6b95, #ffd600); transition: width 0.3s ease; border-radius: 12px; position: relative; }'
				+ '.progress-bar-fill::after { content: ""; position: absolute; top: 0; left: 0; bottom: 0; right: 0; background: linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent); animation: shimmer 1.5s infinite; }'
				+ '@keyframes shimmer { 0% { transform: translateX(-100%); } 100% { transform: translateX(100%); } }'
				+ '.progress-text { text-align: center; margin-top: 10px; font-size: 14px; color: #555; font-weight: 500; }'
				+ '.progress-percent { display: block; font-size: 24px; color: #ff6b95; font-weight: bold; margin-bottom: 5px; }'
				+ '.progress-message { color: #666; font-size: 13px; line-height: 1.6; margin-top: 8px; }'
				+ '.result-icon { font-size: 48px; text-align: center; margin-bottom: 15px; display: block; }'
				+ '.result-success { color: #52c41a; }'
				+ '.result-error { color: #ff4d4f; }'
				+ '.result-msg { font-size: 14px; color: #333; line-height: 1.6; white-space: pre-line; text-align: center; background: #f8f9fa; padding: 15px; border-radius: 8px; }'
				+ '#myLoadingModal { z-index: 10001 !important; }'
				+ '#myResultModal { z-index: 10002 !important; }';

			if (!dynamicLoading.checkCss('labelPrintingBtn')) {
				dynamicLoading.insertCss(innerTexts, 'labelPrintingBtn');
			}
			this.appendDom(el);
			this.createGlobalModals();
		},

		appendDom: function (el) {
			var self = this;
			var labelPrint = document.createElement('div');
			var labelPrintIcon, labelPrintToolbar;

			self.buttonElement = labelPrint;
			el.appendChild(labelPrint);

			if (this.place === 'toolbar') {
				labelPrint.parentNode.setAttribute('class', 'labelPrintingBtnBox');
				labelPrintIcon = document.createElement('i');
				labelPrintIcon.setAttribute('class', 'CAP cap-icon-xiazai');
				labelPrint.appendChild(labelPrintIcon);
				labelPrintToolbar = document.createElement('span');
				labelPrintToolbar.innerHTML = this.data && this.data.name ? this.data.name : '同步回单';
				labelPrint.appendChild(labelPrintToolbar);
			} else {
				labelPrint.innerHTML = this.data && this.data.name ? this.data.name : '';
				labelPrint.setAttribute('class', 'labelPrintingBtnRow labelPrintingBtnHandler');
			}

			labelPrint.onclick = function (e) {
				if (self.isLoading) return;
				if (e && e.stopPropagation) e.stopPropagation();
				self.openDateModal();
			};
			labelPrint.parentNode.onclick = function (e) { e.stopPropagation(); }
		},

		createGlobalModals: function () {
			if (document.getElementById('myLoadingModal')) return;
			var loadingOverlay = document.createElement('div');
			loadingOverlay.id = 'myLoadingModal';
			loadingOverlay.className = 'custom-modal-overlay';
			loadingOverlay.innerHTML = ''
				+ '<div class="custom-modal-box" style="width: 400px; text-align: center;">'
				+ '  <div class="custom-modal-title" style="margin-bottom: 20px;">🏦 正在同步银行回单</div>'
				+ '  <div class="progress-container">'
				+ '    <div class="progress-bar-bg">'
				+ '      <div id="progressBarFill" class="progress-bar-fill" style="width: 0%;"></div>'
				+ '    </div>'
				+ '    <div class="progress-text">'
				+ '      <span id="progressPercent" class="progress-percent">0%</span>'
				+ '      <div id="progressMessage" class="progress-message">正在初始化...</div>'
				+ '    </div>'
				+ '  </div>'
				+ '</div>';
			document.body.appendChild(loadingOverlay);

			var resultOverlay = document.createElement('div');
			resultOverlay.id = 'myResultModal';
			resultOverlay.className = 'custom-modal-overlay';
			resultOverlay.innerHTML = ''
				+ '<div class="custom-modal-box">'
				+ '  <i id="resultIcon" class="result-icon"></i>'
				+ '  <div id="resultTitle" class="custom-modal-title"></div>'
				+ '  <div id="resultMsg" class="result-msg"></div>'
				+ '  <div class="custom-modal-btns">'
				+ '    <button id="resultBtnConfirm" class="custom-btn custom-btn-confirm">知道了</button>'
				+ '  </div>'
				+ '</div>';
			document.body.appendChild(resultOverlay);
		},

		showLoading: function (show) {
			var el = document.getElementById('myLoadingModal');
			if (show) {
				el.classList.add('active');
				// 重置进度条
				this.updateProgress(0, '正在初始化...');
			} else {
				el.classList.remove('active');
			}
		},

		updateProgress: function (percent, message) {
			var fillEl = document.getElementById('progressBarFill');
			var percentEl = document.getElementById('progressPercent');
			var msgEl = document.getElementById('progressMessage');

			if (fillEl) fillEl.style.width = percent + '%';
			if (percentEl) percentEl.innerText = percent + '%';
			if (msgEl) msgEl.innerText = message || '处理中...';
		},

		showResult: function (isSuccess, message, callback) {
			var el = document.getElementById('myResultModal');
			var icon = document.getElementById('resultIcon');
			var title = document.getElementById('resultTitle');
			var msg = document.getElementById('resultMsg');
			var btn = document.getElementById('resultBtnConfirm');

			if (isSuccess) {
				icon.className = 'result-icon result-success CAP cap-icon-wancheng';
				icon.innerHTML = '✔';
				title.innerText = '同步完成';
			} else {
				icon.className = 'result-icon result-error';
				icon.innerHTML = '✘';
				title.innerText = '同步失败';
			}

			msg.innerHTML = message.replace(/\n/g, '<br/>');
			el.classList.add('active');
			btn.onclick = function () {
				el.classList.remove('active');
				if (callback) callback();
			};
		},

		openDateModal: function () {
			var self = this;
			var existingModal = document.getElementById('myDateModal');
			var today = new Date().toISOString().split('T')[0];

			if (!existingModal) {
				var overlay = document.createElement('div');
				overlay.id = 'myDateModal';
				overlay.className = 'custom-modal-overlay';
				var box = document.createElement('div');
				box.className = 'custom-modal-box';

				box.innerHTML = ''
					+ '<div class="custom-modal-title">📝 银行回单同步设置</div>'
					+ '<div class="custom-input-group"><label class="custom-input-label">开始日期</label><input type="date" id="customStartDate" class="custom-form-input" max="' + today + '"></div>'
					+ '<div class="custom-input-group"><label class="custom-input-label">结束日期</label><input type="date" id="customEndDate" class="custom-form-input" max="' + today + '"></div>'
					+ '<div class="custom-input-group"><label class="custom-input-label">所属公司</label><select id="customCompany" class="custom-form-input"><option value="" disabled selected hidden>请选择所属公司</option><option value="致远">致远</option><option value="搭见">搭见</option></select></div>'
					+ '<div class="custom-modal-btns"><button id="customBtnCancel" class="custom-btn custom-btn-cancel">取消</button><button id="customBtnConfirm" class="custom-btn custom-btn-confirm">开始同步</button></div>';

				overlay.appendChild(box);
				document.body.appendChild(overlay);

				document.getElementById('customBtnCancel').onclick = function () {
					document.getElementById('myDateModal').classList.remove('active');
				};

				document.getElementById('customBtnConfirm').onclick = function () {
					self.handleConfirm();
				};

				existingModal = overlay;
			}

			setTimeout(function () { existingModal.classList.add('active'); }, 10);
		},

		handleConfirm: function () {
			var self = this;
			var sDate = document.getElementById('customStartDate').value;
			var eDate = document.getElementById('customEndDate').value;
			var company = document.getElementById('customCompany').value;
			var today = new Date().toISOString().split('T')[0];

			if (!sDate || !eDate || !company) {
				self.showResult(false, "请将所有信息填写完整。");
				return;
			}
			if (sDate > eDate) {
				self.showResult(false, "开始日期不能晚于结束日期。");
				return;
			}
			if (eDate > today) {
				self.showResult(false, "结束日期不能大于今天 (" + today + ")。");
				return;
			}

			// 1. 兼容性处理
			var d1 = new Date(sDate.replace(/-/g, '/'));
			var d2 = new Date(eDate.replace(/-/g, '/'));

			// 2. 计算天数差
			var timeDiff = d2.getTime() - d1.getTime();
			var days = Math.floor(timeDiff / (1000 * 3600 * 24));

			// 3. 增加 isNaN 判断
			if (isNaN(days) || days > 100) {
				self.showResult(false, "一次最多同步 100 天的数据。");
				return;
			}

			document.getElementById('myDateModal').classList.remove('active');
			self.implementClick(sDate, eDate, company);
		},

		implementClick: async function (startDate, endDate, company) {
			var self = this;
			if (self.isLoading) return;

			var formId = getQueryString("formId");
			if (!formId && self.adaptation && self.adaptation.formMessage) {
				formId = self.adaptation.formMessage.formId || self.adaptation.formMessage.contentTemplateId;
			}

			if (!formId) {
				self.showResult(false, "无法获取当前表单ID，请尝试刷新页面。");
				return;
			}
			console.log('抓取到的formId:', formId);

			self.isLoading = true;
			self.showLoading(true);

			try {
				// === 1. 调用 startSync.do 启动异步任务 ===
				var startUrl = '/seeyon/dj/startSync.do?startDate=' + startDate
					+ '&endDate=' + endDate
					+ '&company=' + encodeURIComponent(company)
					+ '&formId=' + formId;

				var startResponse = await fetch(startUrl);
				if (!startResponse.ok) throw new Error("启动任务失败: HTTP " + startResponse.status);

				var startData = await startResponse.json();
				if (!startData.success) {
					self.showLoading(false);
					self.showResult(false, startData.message || "启动任务失败");
					self.isLoading = false;
					return;
				}

				var taskId = startData.taskId;
				if (!taskId) {
					self.showLoading(false);
					self.showResult(false, "未能获取任务ID");
					self.isLoading = false;
					return;
				}

				console.log('任务已启动，taskId:', taskId);

				// === 2. 轮询 checkProgress.do 获取进度 ===
				var pollInterval = 500; // 每500ms轮询一次
				var maxPolls = 300; // 最多轮询150秒(500ms * 300)
				var pollCount = 0;

				var pollProgress = function () {
					if (pollCount >= maxPolls) {
						self.showLoading(false);
						self.showResult(false, "任务超时，请稍后查看结果或重试");
						self.isLoading = false;
						return;
					}

					pollCount++;

					fetch('/seeyon/dj/checkProgress.do?taskId=' + taskId)
						.then(function (response) {
							if (!response.ok) throw new Error("查询进度失败");
							return response.json();
						})
						.then(function (progress) {
							// progress 结构: { status, percent, message }
							// status: RUNNING, SUCCESS, ERROR, UNKNOWN

							console.log('进度:', progress.status, progress.percent + '%', progress.message);

							// 更新进度条
							self.updateProgress(progress.percent, progress.message);

							if (progress.status === 'SUCCESS') {
								// 任务成功
								self.showLoading(false);
								self.showResult(true, progress.message || "同步完成", function () {
									window.location.reload();
								});
								self.isLoading = false;
							} else if (progress.status === 'ERROR' || progress.status === 'UNKNOWN') {
								// 任务失败或未知
								self.showLoading(false);
								var errorMsg = progress.message || "任务执行失败";
								if (errorMsg.indexOf("DCAT003") > -1 || errorMsg.indexOf("白名单") > -1) {
									errorMsg = "❌ 银行拒绝访问：您的服务器 IP 未在白名单中。\n\n请联系技术人员将服务器公网 IP 加入招商银行 CDC 白名单。";
								}
								self.showResult(false, errorMsg);
								self.isLoading = false;
							} else if (progress.status === 'RUNNING') {
								// 继续轮询
								setTimeout(pollProgress, pollInterval);
							} else {
								// 未知状态，继续轮询
								setTimeout(pollProgress, pollInterval);
							}
						})
						.catch(function (error) {
							// 轮询出错，继续重试
							console.error('轮询异常:', error);
							setTimeout(pollProgress, pollInterval);
						});
				};

				// 开始第一次轮询
				pollProgress();

			} catch (error) {
				self.showLoading(false);
				self.showResult(false, '启动任务时发生异常: ' + error.message);
				self.isLoading = false;
			}
		}
	};
	return labelPrintingBtn;
});