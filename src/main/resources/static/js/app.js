(function () {
  'use strict';
  var main = document.getElementById('main');

  /* ---------- Theme toggle ---------- */
  var root = document.documentElement;
  var toggle = document.getElementById('themeToggle');
  function syncToggle() {
    var dark = root.getAttribute('data-theme') === 'dark';
    toggle.setAttribute('aria-label', dark ? 'Switch to light mode' : 'Switch to dark mode');
    toggle.setAttribute('aria-pressed', String(dark));
  }
  toggle.addEventListener('click', function () {
    var next = root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
    root.setAttribute('data-theme', next);
    try { localStorage.setItem('theme', next); } catch (e) {}
    syncToggle();
  });
  syncToggle();

  /* ---------- Helpers ---------- */
  function el(tag, attrs, children) {
    var n = document.createElement(tag);
    Object.keys(attrs || {}).forEach(function (k) {
      if (k === 'text') n.textContent = attrs[k];
      else if (k.indexOf('on') === 0) n.addEventListener(k.slice(2), attrs[k]);
      else n.setAttribute(k, attrs[k]);
    });
    (children || []).forEach(function (c) { if (c) n.appendChild(typeof c === 'string' ? document.createTextNode(c) : c); });
    return n;
  }
  function voterId() {
    var id = null;
    try { id = localStorage.getItem('voterId'); } catch (e) {}
    if (!id) {
      id = (window.crypto && crypto.randomUUID) ? crypto.randomUUID() : 'v-' + Date.now() + '-' + Math.random().toString(36).slice(2);
      try { localStorage.setItem('voterId', id); } catch (e) {}
    }
    return id;
  }
  function toast(msg) {
    var t = el('div', { 'class': 'toast', text: msg });
    document.getElementById('toasts').appendChild(t);
    setTimeout(function () { t.remove(); }, 3500);
  }
  function api(path, opts) {
    opts = opts || {};
    var init = { method: opts.method || 'GET', headers: { 'Accept': 'application/json' } };
    if (opts.body !== undefined) { init.headers['Content-Type'] = 'application/json'; init.body = JSON.stringify(opts.body); }
    return fetch(path, init).then(function (r) {
      return r.text().then(function (txt) {
        var data = null;
        try { data = txt ? JSON.parse(txt) : null; } catch (e) {}
        if (!r.ok) {
          var msg = data && (data.message || data.error) || ('Request failed (' + r.status + ')');
          var err = new Error(typeof msg === 'string' ? msg : JSON.stringify(msg));
          err.status = r.status; err.code = data && data.error; throw err;
        }
        return data;
      });
    }, function () { throw new Error('Cannot reach the server. Check that the application is running.'); });
  }
  /* Tolerant normalisers so the UI works with small differences in API shape */
  function isActive(p) {
    if (typeof p.active === 'boolean') return p.active;
    return String(p.status || 'ACTIVE').toUpperCase() === 'ACTIVE';
  }
  function optText(o) { return o.text || o.label || o.name || ''; }
  function optId(o) { return o.id || o.optionId; }
  function listOf(d, key) { return Array.isArray(d) ? d : (d && (d[key] || d.items || d.data)) || []; }
  function badge(p) { var a = isActive(p); return el('span', { 'class': 'badge ' + (a ? 'active' : 'closed'), text: a ? 'Active' : 'Closed' }); }
  function setNav(name) {
    document.querySelectorAll('[data-nav]').forEach(function (a) { a.classList.toggle('active', a.getAttribute('data-nav') === name); });
  }
  function show(nodes) { main.replaceChildren.apply(main, nodes); main.focus({ preventScroll: true }); window.scrollTo(0, 0); }
  function errorState(msg, retry) {
    return el('div', { 'class': 'state' }, [el('h3', { text: 'Something went wrong' }), el('p', { text: msg }), el('button', { 'class': 'btn primary', type: 'button', text: 'Try again', onclick: retry })]);
  }

  /* ---------- Home: dashboard + polls ---------- */
  function renderHome() {
    setNav('home');
    var statsBox = el('section', { 'class': 'stats', 'aria-label': 'Dashboard' });
    var listBox = el('div', {}, [el('div', { 'class': 'skeleton' }), el('div', { 'class': 'skeleton' }), el('div', { 'class': 'skeleton' })]);
    show([
      el('div', { 'class': 'page-head' }, [
        el('div', {}, [el('h1', { text: 'Polls' }), el('p', { text: 'Pick a poll to cast your vote or see how it is going.' })]),
        el('a', { 'class': 'btn primary', href: '#/create', text: 'Create a poll' })
      ]),
      statsBox, el('h2', { text: 'Available polls' }), listBox
    ]);
    load();
    function load() {
      api('/api/polls').then(function (d) {
        var polls = listOf(d, 'polls');
        return api('/api/stats').catch(function () { return null; }).then(function (s) { return { polls: polls, stats: s }; });
      }).then(function (r) {
        var polls = r.polls, s = r.stats || {};
        var active = polls.filter(isActive).length;
        var votes = s.totalVotes != null ? s.totalVotes : polls.reduce(function (n, p) { return n + (p.totalVotes || 0); }, 0);
        var cards = [
          ['Total polls', s.totalPolls != null ? s.totalPolls : polls.length, true],
          ['Active polls', s.activePolls != null ? s.activePolls : active],
          ['Closed polls', s.closedPolls != null ? s.closedPolls : polls.length - active],
          ['Votes recorded', votes]
        ];
        statsBox.replaceChildren.apply(statsBox, cards.map(function (c) {
          return el('div', { 'class': 'stat' + (c[2] ? ' hl' : '') }, [el('b', { text: String(c[1]) }), el('span', { text: c[0] })]);
        }));
        if (!polls.length) {
          listBox.replaceChildren(el('div', { 'class': 'state' }, [el('h3', { text: 'No polls yet' }), el('p', { text: 'Create the first poll and share it with voters.' }), el('a', { 'class': 'btn primary', href: '#/create', text: 'Create a poll' })]));
          return;
        }
        var ul = el('ul', { 'class': 'poll-list' }, polls.map(function (p) {
          var n = (p.options || []).length;
          return el('li', {}, [el('a', { 'class': 'poll-item', href: '#/poll/' + encodeURIComponent(p.id) }, [
            el('div', {}, [el('strong', { text: p.question }), el('small', { text: n + ' options' })]), badge(p)
          ])]);
        }));
        listBox.replaceChildren(ul);
      }).catch(function (e) {
        statsBox.replaceChildren();
        listBox.replaceChildren(errorState(e.message, renderHome));
      });
    }
  }

  /* ---------- Create poll ---------- */
  function renderCreate() {
    setNav('create');
    var q = el('input', { type: 'text', id: 'q', maxlength: '200', placeholder: 'What should the team have for lunch?', 'aria-describedby': 'qErr' });
    var qErr = el('div', { 'class': 'error-text', id: 'qErr' });
    var optsBox = el('div', {});
    var optErr = el('div', { 'class': 'error-text' });
    var banner = el('div', {});
    var submit = el('button', { 'class': 'btn primary', type: 'submit', text: 'Create poll' });

    function addOpt(val) {
      var row = el('div', { 'class': 'opt-row' });
      var inp = el('input', { type: 'text', maxlength: '100', placeholder: 'Option', 'aria-label': 'Option text' });
      inp.value = val || '';
      var rm = el('button', { 'class': 'btn sm', type: 'button', 'aria-label': 'Remove option', text: 'Remove', onclick: function () {
        if (optsBox.children.length <= 2) { optErr.textContent = 'A poll needs at least two options.'; return; }
        row.remove(); optErr.textContent = '';
      } });
      row.appendChild(inp); row.appendChild(rm); optsBox.appendChild(row);
    }
    addOpt(); addOpt();

    var form = el('form', { 'class': 'card', novalidate: 'novalidate' }, [
      banner,
      el('div', { 'class': 'field' }, [el('label', { 'for': 'q', text: 'Question' }), q, qErr]),
      el('div', { 'class': 'field' }, [el('div', { 'class': 'label', text: 'Options' }), optsBox, optErr,
        el('button', { 'class': 'btn sm', type: 'button', text: 'Add option', onclick: function () { addOpt(); } }),
        el('div', { 'class': 'hint', text: 'Add at least two options.' })]),
      el('div', { 'class': 'actions' }, [submit, el('a', { 'class': 'btn', href: '#/', text: 'Cancel' })])
    ]);
    form.addEventListener('submit', function (ev) {
      ev.preventDefault();
      banner.replaceChildren(); qErr.textContent = ''; optErr.textContent = ''; q.removeAttribute('aria-invalid');
      var question = q.value.trim();
      var options = Array.prototype.map.call(optsBox.querySelectorAll('input'), function (i) { return i.value.trim(); });
      var ok = true;
      if (!question) { qErr.textContent = 'Enter a question.'; q.setAttribute('aria-invalid', 'true'); ok = false; }
      if (options.some(function (o) { return !o; })) { optErr.textContent = 'Fill in every option or remove the empty ones.'; ok = false; }
      else if (options.length < 2) { optErr.textContent = 'Add at least two options.'; ok = false; }
      if (!ok) return;
      submit.disabled = true; submit.replaceChildren(el('span', { 'class': 'spinner' }), ' Creating…');
      api('/api/polls', { method: 'POST', body: { question: question, options: options } }).then(function (p) {
        toast('Poll created');
        location.hash = '#/poll/' + encodeURIComponent(p.id);
      }).catch(function (e) {
        banner.replaceChildren(el('div', { 'class': 'notice error', role: 'alert', text: e.message }));
        submit.disabled = false; submit.textContent = 'Create poll';
      });
    });
    show([el('a', { 'class': 'back', href: '#/', text: '‹ All polls' }), el('div', { 'class': 'page-head' }, [el('div', {}, [el('h1', { text: 'Create a poll' }), el('p', { text: 'Ask one question and give voters clear choices.' })])]), form]);
  }

  /* ---------- Poll: vote + results ---------- */
  function renderPoll(id) {
    setNav('home');
    show([el('a', { 'class': 'back', href: '#/', text: '‹ All polls' }), el('div', { 'class': 'skeleton' }), el('div', { 'class': 'skeleton' })]);
    function load() {
      Promise.all([api('/api/polls/' + encodeURIComponent(id)), api('/api/polls/' + encodeURIComponent(id) + '/results')]).then(function (r) {
        draw(r[0], r[1]);
      }).catch(function (e) {
        show([el('a', { 'class': 'back', href: '#/', text: '‹ All polls' }), e.status === 404
          ? el('div', { 'class': 'state' }, [el('h3', { text: 'Poll not found' }), el('p', { text: 'It may have been removed or the link is wrong.' }), el('a', { 'class': 'btn primary', href: '#/', text: 'Browse polls' })])
          : errorState(e.message, load)]);
      });
    }
    function draw(poll, res) {
      var active = isActive(poll);
      var banner = el('div', {});
      var voteCard;
      if (active) {
        var fs = el('fieldset', { 'class': 'ballot' }, [el('legend', { text: 'Choose one option' })]);
        (poll.options || []).forEach(function (o) {
          fs.appendChild(el('label', { 'class': 'choice' }, [
            el('input', { type: 'radio', name: 'opt', value: optId(o) }),
            el('span', { 'class': 'box', 'aria-hidden': 'true' }, [svgCheck()]),
            el('span', { text: optText(o) })
          ]));
        });
        var btn = el('button', { 'class': 'btn primary', type: 'submit', text: 'Submit vote' });
        var vf = el('form', {}, [banner, fs, el('div', { 'class': 'actions', style: 'margin-top:0' }, [btn])]);
        vf.addEventListener('submit', function (ev) {
          ev.preventDefault();
          var sel = vf.querySelector('input[name=opt]:checked');
          if (!sel) { banner.replaceChildren(el('div', { 'class': 'notice error', role: 'alert', text: 'Select an option before voting.' })); return; }
          btn.disabled = true; btn.replaceChildren(el('span', { 'class': 'spinner' }), ' Submitting…');
          api('/api/polls/' + encodeURIComponent(id) + '/votes', { method: 'POST', body: { voterId: voterId(), optionId: sel.value } }).then(function () {
            toast('Vote recorded'); load();
          }).catch(function (e) {
            var msg = e.code === 'POLL_CLOSED' ? 'This poll is closed.' : e.code === 'DUPLICATE_VOTE' ? 'You have already voted in this poll.' : e.message;
            banner.replaceChildren(el('div', { 'class': 'notice error', role: 'alert', text: msg }));
            btn.disabled = false; btn.textContent = 'Submit vote';
          });
        });
        voteCard = el('section', { 'class': 'card' }, [el('h2', { text: 'Cast your vote' }), vf]);
      } else {
        voteCard = el('section', { 'class': 'card' }, [el('h2', { text: 'Voting is closed' }), el('p', { text: 'This poll no longer accepts votes. The final results are shown here.' })]);
      }

      var items = listOf(res, 'options');
      var total = res.totalVotes != null ? res.totalVotes : items.reduce(function (n, o) { return n + (o.votes || o.count || 0); }, 0);
      var resList = el('ul', { 'class': 'results' }, items.map(function (o) {
        var v = o.votes != null ? o.votes : (o.count || 0);
        var pct = o.percentage != null ? o.percentage : (total ? (v * 100 / total) : 0);
        var shown = Math.round(pct * 10) / 10;
        var fill = el('i', {});
        setTimeout(function () { fill.style.width = (total ? pct : 0) + '%'; }, 30);
        return el('li', {}, [
          el('div', { 'class': 'res-top' }, [el('span', { text: o.text || o.label || optText(o) }), el('span', { text: v + (v === 1 ? ' vote · ' : ' votes · ') + shown + '%' })]),
          el('div', { 'class': 'bar', role: 'progressbar', 'aria-valuenow': String(shown), 'aria-valuemin': '0', 'aria-valuemax': '100' }, [fill])
        ]);
      }));
      var resCard = el('section', { 'class': 'card' }, [el('h2', { text: 'Results' }), resList,
        el('p', { 'class': 'res-total', text: total ? total + ' total ' + (total === 1 ? 'vote' : 'votes') : 'No votes yet. Be the first to vote.' })]);

      var closeBtn = active ? el('button', { 'class': 'btn danger sm', type: 'button', text: 'Close poll', onclick: function () {
        if (!confirm('Close this poll? No one will be able to vote afterwards.')) return;
        api('/api/polls/' + encodeURIComponent(id) + '/close', { method: 'POST' }).then(function () { toast('Poll closed'); load(); })
          .catch(function (e) { toast(e.message); });
      } }) : null;

      show([el('a', { 'class': 'back', href: '#/', text: '‹ All polls' }),
        el('div', { 'class': 'page-head' }, [el('div', {}, [el('h1', { text: poll.question })]), el('div', {}, [badge(poll)])]),
        el('div', { 'class': 'grid-2' }, [voteCard, resCard]),
        closeBtn ? el('div', { 'class': 'actions' }, [closeBtn]) : null]);
    }
    load();
  }
  function svgCheck() {
    var ns = 'http://www.w3.org/2000/svg';
    var s = document.createElementNS(ns, 'svg'); s.setAttribute('width', '14'); s.setAttribute('height', '14'); s.setAttribute('viewBox', '0 0 24 24');
    var p = document.createElementNS(ns, 'path'); p.setAttribute('d', 'M5 12.5l4.5 4.5L19 7'); p.setAttribute('fill', 'none'); p.setAttribute('stroke', 'var(--accent-ink)'); p.setAttribute('stroke-width', '3.5'); p.setAttribute('stroke-linecap', 'round'); p.setAttribute('stroke-linejoin', 'round');
    s.appendChild(p); return s;
  }

  /* ---------- Router ---------- */
  function route() {
    var h = location.hash.replace(/^#/, '') || '/';
    var m;
    if (h === '/create') renderCreate();
    else if ((m = h.match(/^\/poll\/(.+)$/))) renderPoll(decodeURIComponent(m[1]));
    else renderHome();
  }
  window.addEventListener('hashchange', route);
  route();
})();
