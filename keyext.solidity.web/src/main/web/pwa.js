const $ = (id) => document.getElementById(id);

const standalone = () => matchMedia('(display-mode: standalone)').matches || navigator.standalone === true;
const appleMobile = () => /iphone|ipad|ipod/i.test(navigator.userAgent)
  || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);

function setupInstall(toast) {
  let deferred = null;
  window.addEventListener('beforeinstallprompt', (event) => {
    event.preventDefault();
    deferred = event;
    $('install').hidden = false;
  });
  window.addEventListener('appinstalled', () => {
    deferred = null;
    $('install').hidden = true;
    toast('SolKey is installed.');
  });
  if (appleMobile() && !standalone()) $('install').hidden = false;
  $('install').addEventListener('click', async () => {
    if (!deferred) {
      toast('Tap Share, then “Add to Home Screen”.');
      return;
    }
    deferred.prompt();
    const { outcome } = await deferred.userChoice;
    deferred = null;
    if (outcome === 'accepted') $('install').hidden = true;
  });
}

function setView(view) {
  document.body.dataset.view = view;
  for (const tab of document.querySelectorAll('.tabbar-item')) {
    const active = tab.dataset.view === view;
    tab.classList.toggle('active', active);
    tab.setAttribute('aria-pressed', String(active));
  }
}

function setupTabs() {
  setView('code');
  for (const tab of document.querySelectorAll('.tabbar-item')) {
    tab.addEventListener('click', () => {
      setView(tab.dataset.view);
      scrollTo({ top: 0 });
    });
  }
  $('tabVerify').addEventListener('click', () => {
    setView('results');
    $('verify').click();
  });
  $('functions').addEventListener('click', (event) => {
    if (event.target.closest('button.link')) setView('code');
  });

  const syncVerify = () => { $('tabVerify').disabled = $('verify').disabled; };
  new MutationObserver(syncVerify).observe($('verify'), { attributes: true, attributeFilter: ['disabled'] });
  syncVerify();

  const syncCount = () => {
    const count = !$('summary').hidden && $('summary').textContent.match(/^(\d+)\/(\d+)/);
    $('tabCount').hidden = !count;
    if (!count) return;
    $('tabCount').textContent = `${count[1]}/${count[2]}`;
    $('tabCount').className = `tabbar-count ${count[1] === count[2] ? 'pass' : 'fail'}`;
  };
  new MutationObserver(syncCount).observe($('summary'), { attributes: true, childList: true, characterData: true, subtree: true });
}

function setupFileLaunch() {
  if (!('launchQueue' in window)) return;
  window.launchQueue.setConsumer(async (params) => {
    if (!params.files?.length) return;
    const files = new DataTransfer();
    for (const handle of params.files) files.items.add(await handle.getFile());
    $('file').files = files.files;
    $('file').dispatchEvent(new Event('change'));
  });
}

export function setupApp({ toast }) {
  setupInstall(toast);
  setupTabs();
  setupFileLaunch();
}

export async function shareLink(url, copy) {
  if (navigator.share && matchMedia('(pointer: coarse)').matches) {
    try {
      await navigator.share({ title: 'SolKey contract', url });
      return;
    } catch (e) {
      if (e.name === 'AbortError') return;
    }
  }
  await copy();
}
