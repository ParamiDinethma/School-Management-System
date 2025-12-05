class ReportTemplatesPage {
    constructor() {
        this.currentId = null;
        this.loadTemplates();
    }

    async loadTemplates() {
        try {
            const res = await fetch('/admin/report-templates/api');
            const data = await res.json();
            const list = data.templates || [];
            const tbody = document.getElementById('templatesTableBody');
            if (list.length === 0) {
                tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-4">No templates found</td></tr>';
                return;
            }
            tbody.innerHTML = list.map(t => `
                <tr>
                    <td>${this.escape(t.templateName)}</td>
                    <td>${t.active ? '<span class="badge bg-success">Active</span>' : '<span class="badge bg-secondary">Inactive</span>'}</td>
                    <td>${t.createdAt ? new Date(t.createdAt).toLocaleString() : '-'}</td>
                    <td class="text-end">
                        <button class="btn btn-sm btn-outline-primary me-2" onclick="reportTemplates.openEdit(${t.id})">Edit</button>
                        <button class="btn btn-sm btn-outline-danger" onclick="reportTemplates.remove(${t.id})">Delete</button>
                    </td>
                </tr>
            `).join('');
        } catch (e) {
            console.error('Failed to load templates', e);
        }
    }

    openCreate() {
        this.currentId = null;
        document.getElementById('templateModalLabel').textContent = 'Create Template';
        document.getElementById('templateForm').reset();
        document.getElementById('isActive').checked = true;
    }

    async openEdit(id) {
        try {
            const res = await fetch(`/admin/report-templates/api/${id}`);
            const data = await res.json();
            const t = data.template;
            this.currentId = t.id;
            document.getElementById('templateModalLabel').textContent = 'Edit Template';
            document.getElementById('templateId').value = t.id;
            document.getElementById('templateName').value = t.templateName || '';
            document.getElementById('headerText').value = t.headerText || '';
            document.getElementById('footerText').value = t.footerText || '';
            document.getElementById('isActive').checked = !!t.active;
            new bootstrap.Modal(document.getElementById('templateModal')).show();
        } catch (e) {
            console.error('Failed to load template', e);
        }
    }

    async save() {
        const payload = {
            templateName: document.getElementById('templateName').value.trim(),
            headerText: document.getElementById('headerText').value,
            footerText: document.getElementById('footerText').value,
            active: document.getElementById('isActive').checked
        };
        if (!payload.templateName) {
            alert('Template name is required');
            return;
        }
        const isEdit = !!this.currentId;
        const url = isEdit ? `/admin/report-templates/api/${this.currentId}` : '/admin/report-templates/api';
        const method = isEdit ? 'PUT' : 'POST';
        try {
            const res = await fetch(url, { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
            if (!res.ok) {
                const text = await res.text().catch(() => '');
                throw new Error(text || 'Request failed');
            }
            bootstrap.Modal.getInstance(document.getElementById('templateModal'))?.hide();
            this.loadTemplates();
        } catch (e) {
            console.error('Save failed', e);
            alert('Failed to save template: ' + (e.message || ''));
        }
    }

    async remove(id) {
        if (!confirm('Delete this template?')) return;
        try {
            const res = await fetch(`/admin/report-templates/api/${id}`, { method: 'DELETE' });
            if (!res.ok) throw new Error('Delete failed');
            this.loadTemplates();
        } catch (e) {
            console.error('Delete failed', e);
            alert('Failed to delete template');
        }
    }

    escape(s) { return (s || '').replace(/[&<>"]+/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c])); }
}

let reportTemplates;
document.addEventListener('DOMContentLoaded', () => {
    reportTemplates = new ReportTemplatesPage();
});


