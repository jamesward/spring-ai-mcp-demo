@param String extAppsJs

<!DOCTYPE html>
<html>
<head>
    <title>Shopping List</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            max-width: 480px; margin: 0 auto; padding: 24px;
            background: #f8f9fa; color: #1a1a2e;
        }
        @media (prefers-color-scheme: dark) {
            body { background: #1a1a2e; color: #e8e8e8; }
            .card { background: #16213e; box-shadow: 0 2px 12px rgba(0,0,0,.4); }
            input { background: #0f3460; color: #e8e8e8; border-color: #533483; }
            input::placeholder { color: #888; }
            .item { border-bottom-color: #0f3460; }
            .empty { color: #888; }
        }
        h1 { font-size: 24px; margin-bottom: 16px; display: flex; align-items: center; gap: 8px; }
        .card {
            background: #fff; border-radius: 12px; padding: 20px;
            box-shadow: 0 2px 12px rgba(0,0,0,.08);
        }
        .add-row { display: flex; gap: 8px; margin-bottom: 16px; }
        input {
            flex: 1; padding: 10px 14px; border: 2px solid #e0e0e0; border-radius: 8px;
            font-size: 15px; outline: none; transition: border-color .2s;
        }
        input:focus { border-color: #533483; }
        .btn {
            padding: 10px 18px; border: none; border-radius: 8px; font-size: 15px;
            cursor: pointer; font-weight: 600; transition: transform .1s, opacity .2s;
        }
        .btn:active { transform: scale(.95); }
        .btn-add { background: #533483; color: #fff; }
        .btn-add:hover { background: #4a2d75; }
        .item {
            display: flex; align-items: center; gap: 10px; padding: 12px 0;
            border-bottom: 1px solid #f0f0f0; animation: slideIn .2s ease;
        }
        .item:last-child { border-bottom: none; }
        @keyframes slideIn { from { opacity: 0; transform: translateY(-8px); } to { opacity: 1; transform: translateY(0); } }
        .checkbox {
            width: 22px; height: 22px; border-radius: 6px; border: 2px solid #533483;
            cursor: pointer; display: flex; align-items: center; justify-content: center;
            flex-shrink: 0; transition: background .2s;
        }
        .checkbox.checked { background: #533483; }
        .checkbox.checked::after { content: '✓'; color: #fff; font-size: 14px; font-weight: bold; }
        .item-name { flex: 1; font-size: 15px; transition: all .2s; }
        .item-name.done { text-decoration: line-through; opacity: .4; }
        .btn-remove {
            background: none; border: none; cursor: pointer; font-size: 18px;
            opacity: .3; transition: opacity .2s; padding: 4px;
        }
        .btn-remove:hover { opacity: 1; color: #e74c3c; }
        .empty { text-align: center; padding: 32px 0; color: #aaa; font-size: 15px; }
        .badge {
            background: #533483; color: #fff; border-radius: 20px;
            padding: 2px 10px; font-size: 13px; font-weight: 600;
        }
    </style>
</head>
<body>

<h1>🛒 Shopping List <span class="badge" id="count">0</span></h1>
<div class="card">
    <div class="add-row">
        <input type="text" id="item-input" placeholder="Add an item..." />
        <button class="btn btn-add" id="add-btn">Add</button>
    </div>
    <div id="list"></div>
    <div class="empty" id="empty-msg">Your list is empty</div>
</div>

<script id="ext-apps-src" type="text/plain">
$unsafe{extAppsJs}
</script>

<script type="module">
    const src = document.getElementById('ext-apps-src').textContent;
    const blob = new Blob([src], { type: 'application/javascript' });
    const url = URL.createObjectURL(blob);
    const { App } = await import(url);
    URL.revokeObjectURL(url);

    const app = new App({ name: "shopping-list", version: "1.0.0" });
    await app.connect();

    let items = [];

    const listEl = document.getElementById('list');
    const emptyEl = document.getElementById('empty-msg');
    const countEl = document.getElementById('count');
    const input = document.getElementById('item-input');

    function render() {
        listEl.innerHTML = '';
        const remaining = items.filter(i => !i.done).length;
        countEl.textContent = remaining + '/' + items.length;
        emptyEl.style.display = items.length ? 'none' : 'block';

        items.forEach((item, idx) => {
            const row = document.createElement('div');
            row.className = 'item';
            row.innerHTML =
                '<div class="checkbox ' + (item.done ? 'checked' : '') + '"></div>' +
                '<span class="item-name ' + (item.done ? 'done' : '') + '">' + item.name + '</span>' +
                '<button class="btn-remove">✕</button>';
            row.querySelector('.checkbox').onclick = () => toggle(idx);
            row.querySelector('.btn-remove').onclick = () => remove(idx);
            listEl.appendChild(row);
        });

        syncContext();
    }

    function addItem(name) {
        if (!name.trim()) return;
        items.push({ name: name.trim(), done: false });
        render();
    }

    function toggle(idx) { items[idx].done = !items[idx].done; render(); }
    function remove(idx) { items.splice(idx, 1); render(); }

    async function syncContext() {
        const summary = items.length === 0 ? 'Shopping list is empty' :
            items.map(i => (i.done ? '✓' : '○') + ' ' + i.name).join(', ');
        await app.sendMessage({
            role: "user",
            content: [{ type: "text", text: summary }],
        });
    }

    document.getElementById('add-btn').onclick = () => { addItem(input.value); input.value = ''; input.focus(); };
    input.addEventListener('keydown', e => { if (e.key === 'Enter') { addItem(input.value); input.value = ''; } });

    render();
</script>

</body>
</html>
