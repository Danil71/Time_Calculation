import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import type { AxiosError } from 'axios';
import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/axiosClient';

interface ProgrammingLanguage {
  name: string;
  locPerFp: number;
}

const emptyForm = { name: '', locPerFp: '' };

export default function LanguagesPage() {
  const [languages, setLanguages] = useState<ProgrammingLanguage[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingLanguage, setEditingLanguage] = useState<ProgrammingLanguage | null>(null);
  const [form, setForm] = useState(emptyForm);
  const [formError, setFormError] = useState('');

  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [languageToDelete, setLanguageToDelete] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);

  const fetchLanguages = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await api.get<ProgrammingLanguage[]>('/languages');
      setLanguages(response.data);
    } catch {
      setError('Не удалось загрузить список языков.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchLanguages();
  }, [fetchLanguages]);

  const openCreateDialog = () => {
    setEditingLanguage(null);
    setForm(emptyForm);
    setFormError('');
    setDialogOpen(true);
  };

  const openEditDialog = (lang: ProgrammingLanguage) => {
    setEditingLanguage(lang);
    setForm({ name: lang.name, locPerFp: String(lang.locPerFp) });
    setFormError('');
    setDialogOpen(true);
  };

  const handleSave = async () => {
    const locPerFp = Number(form.locPerFp);
    if (!form.name.trim()) {
      setFormError('Укажите название языка.');
      return;
    }
    if (!Number.isInteger(locPerFp) || locPerFp <= 0) {
      setFormError('LOC/FP должен быть целым положительным числом.');
      return;
    }

    setSaving(true);
    setFormError('');
    try {
      const body = { name: form.name.trim(), locPerFp };
      if (editingLanguage) {
        await api.put(`/languages/${encodeURIComponent(editingLanguage.name)}`, body);
      } else {
        await api.post('/languages', body);
      }
      setDialogOpen(false);
      await fetchLanguages();
    } catch (err: unknown) {
      const axiosErr = err as AxiosError<{ message?: string }>;
      setFormError(axiosErr.response?.data?.message || 'Не удалось сохранить язык.');
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteClick = (name: string) => {
    setLanguageToDelete(name);
    setDeleteDialogOpen(true);
  };

  const handleConfirmDelete = async () => {
    if (!languageToDelete) return;
    setDeleting(true);
    try {
      await api.delete(`/languages/${encodeURIComponent(languageToDelete)}`);
      setDeleteDialogOpen(false);
      setLanguageToDelete(null);
      await fetchLanguages();
    } catch {
      setError('Не удалось удалить язык.');
    } finally {
      setDeleting(false);
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 10 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ mt: 4 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4, flexWrap: 'wrap', gap: 2 }}>
        <Box>
          <Typography variant="h4" fontWeight="bold">
            Языки программирования
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            Справочник LOC/FP для расчёта COCOMO II. Изменения применяются ко всем новым оценкам.
          </Typography>
        </Box>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openCreateDialog}>
          Добавить язык
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Язык</TableCell>
              <TableCell align="right">LOC/FP</TableCell>
              <TableCell align="right" width={120}>Действия</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {languages.length === 0 ? (
              <TableRow>
                <TableCell colSpan={3} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                  Список пуст. Добавьте первый язык.
                </TableCell>
              </TableRow>
            ) : (
              languages.map((lang) => (
                <TableRow key={lang.name} hover>
                  <TableCell>{lang.name}</TableCell>
                  <TableCell align="right">{lang.locPerFp}</TableCell>
                  <TableCell align="right">
                    <IconButton size="small" color="primary" onClick={() => openEditDialog(lang)} title="Изменить">
                      <EditIcon fontSize="small" />
                    </IconButton>
                    <IconButton size="small" color="error" onClick={() => handleDeleteClick(lang.name)} title="Удалить">
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={dialogOpen} onClose={() => !saving && setDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>{editingLanguage ? 'Редактировать язык' : 'Новый язык'}</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
          {formError && <Alert severity="error">{formError}</Alert>}
          <TextField
            label="Название"
            value={form.name}
            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
            fullWidth
            autoFocus
            disabled={saving}
          />
          <TextField
            label="LOC/FP"
            type="number"
            value={form.locPerFp}
            onChange={(e) => setForm((f) => ({ ...f, locPerFp: e.target.value }))}
            fullWidth
            disabled={saving}
            inputProps={{ min: 1, step: 1 }}
            helperText="Строк кода на одну функциональную точку (COCOMO II)"
          />
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setDialogOpen(false)} disabled={saving}>Отмена</Button>
          <Button variant="contained" onClick={handleSave} disabled={saving}>
            {saving ? 'Сохранение...' : 'Сохранить'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={deleteDialogOpen} onClose={() => !deleting && setDeleteDialogOpen(false)}>
        <DialogTitle>Удалить язык?</DialogTitle>
        <DialogContent>
          <Typography>
            Язык «{languageToDelete}» будет удалён из справочника. Существующие снимки кода не изменятся.
          </Typography>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setDeleteDialogOpen(false)} disabled={deleting}>Отмена</Button>
          <Button variant="contained" color="error" onClick={handleConfirmDelete} disabled={deleting}>
            {deleting ? 'Удаление...' : 'Удалить'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
