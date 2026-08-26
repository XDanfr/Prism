package me.xdan.prism.template

import android.webkit.WebView

object UserScriptEngine {
    fun scriptsFor(ids: Set<String>): List<String> = ids.mapNotNull { id -> script(id) }

    private fun script(id: String): String? = when (id) {
        "disable-video-autoplay" -> """
            (() => {
              const stop = () => document.querySelectorAll('video,audio').forEach(m => {
                m.autoplay = false;
                m.removeAttribute('autoplay');
                try { m.pause(); } catch (_) {}
              });
              stop();
              new MutationObserver(stop).observe(document.documentElement, {subtree:true, childList:true, attributes:true});
            })();
        """.trimIndent()
        "force-copy" -> """
            (() => {
              const style = document.createElement('style');
              style.textContent = '* { user-select: text !important; -webkit-user-select: text !important; }';
              document.documentElement.appendChild(style);
              ['copy','cut','contextmenu','selectstart'].forEach(e => document.addEventListener(e, ev => ev.stopImmediatePropagation(), true));
            })();
        """.trimIndent()
        "force-zoom" -> """
            (() => {
              const viewport = document.querySelector('meta[name="viewport"]');
              const content = 'width=device-width, initial-scale=1, maximum-scale=5, user-scalable=yes';
              if (viewport) viewport.setAttribute('content', content);
              else { const m = document.createElement('meta'); m.name = 'viewport'; m.content = content; document.head.appendChild(m); }
            })();
        """.trimIndent()
        "remove-footers" -> """
            (() => {
              const remove = () => document.querySelectorAll('footer, [role="contentinfo"], .footer, #footer').forEach(e => e.remove());
              remove();
              new MutationObserver(remove).observe(document.documentElement, {subtree:true, childList:true});
            })();
        """.trimIndent()
        "google-translate" -> """
            (() => {
              if (document.getElementById('prism-translate-script')) return;
              const s = document.createElement('script');
              s.id = 'prism-translate-script';
              s.src = 'https://translate.google.com/translate_a/element.js?cb=prismTranslateInit';
              window.prismTranslateInit = () => {
                try { new google.translate.TranslateElement({pageLanguage: document.documentElement.lang || 'auto'}, 'prism-translate'); } catch (_) {}
              };
              const c = document.createElement('div');
              c.id = 'prism-translate';
              c.style.cssText = 'position:fixed;top:0;right:0;z-index:2147483647;background:white;';
              document.body.appendChild(c);
              document.head.appendChild(s);
            })();
        """.trimIndent()
        else -> null
    }

    fun inject(webView: WebView, ids: Set<String>) {
        scriptsFor(ids).forEach { webView.evaluateJavascript("javascript:$it", null) }
    }
}
