import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import MergeTypeIcon from '@mui/icons-material/MergeType';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Paper, Table, TableBody, TableCell,
  TableHead, TableRow,
  TextField,
  Typography
} from '@mui/material';
import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api/axiosClient';

// Строгая типизация данных разработчика, которые приходят с Java-бэкенда
interface Contributor {
  id: string;
  primaryEmail: string;
  displayName: string | null;
  totalCommits: number;
  churnFactor: number;
  calculatedPersRating: number;
  aliases: string[] | null;
}

export default function Team() {
  const { id } = useParams<{ id: string }>(); // ID проекта из URL
  
  const [team, setTeam] = useState<Contributor[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  
  const [openMerge, setOpenMerge] = useState(false);
  const [primaryId, setPrimaryId] = useState('');
  const [duplicateId, setDuplicateId] = useState('');
  const[merging, setMerging] = useState(false);

  // Загрузка списка команды с бэкенда
  const fetchTeam = async () => {
    try {
      const response = await api.get(`/projects/${id}/team`);
      setTeam(response.data);
    } catch (err) {
      console.error('Ошибка загрузки команды', err);
      setError('Не удалось загрузить данные команды. Проверьте консоль.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTeam();
  }, [id]);

  // Отправка запроса на склейку двух профилей
  const handleMerge = async () => {
    if (!primaryId || !duplicateId) {
      alert('Выберите оба профиля!');
      return;
    }
    if (primaryId === duplicateId) {
      alert('Нельзя объединить профиль сам с собой!');
      return;
    }

    setMerging(true);
    try {
      await api.post(`/projects/${id}/team/merge?primaryId=${primaryId}&duplicateId=${duplicateId}`);
      setOpenMerge(false);
      setPrimaryId('');
      setDuplicateId('');
      fetchTeam(); // Перезагружаем таблицу, чтобы увидеть изменения
    } catch (err) {
      console.error('Ошибка объединения:', err);
      alert('Ошибка при объединении профилей.');
    } finally {
      setMerging(false);
    }
  };

  if (loading) return <Box sx={{ mt: 10, textAlign: 'center' }}><CircularProgress /></Box>;

  return (
    <Box sx={{ mt: 4 }}>
      {/* Навигация и Шапка */}
      <Button 
        component={Link} 
        to={`/project/${id}`} 
        startIcon={<ArrowBackIcon />} 
        sx={{ mb: 2 }}
        color="inherit"
      >
        Назад к проекту
      </Button>

      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" fontWeight="bold">Команда проекта</Typography>
        <Button 
          variant="contained" 
          color="secondary" 
          startIcon={<MergeTypeIcon />}
          onClick={() => setOpenMerge(true)}
          disabled={team.length < 2} // Кнопка неактивна, если склеивать некого
        >
          Объединить дубликаты
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      {team.length === 0 ? (
        <Alert severity="info">
          Команда пуста. Убедитесь, что вы запустили синхронизацию с Git на странице проекта.
        </Alert>
      ) : (
        <Paper sx={{ overflowX: 'auto', borderRadius: 2, border: '1px solid #1F2937', boxShadow: 'none' }}>
          <Table>
            <TableHead sx={{ backgroundColor: 'rgba(255, 255, 255, 0.05)' }}>
              <TableRow>
                <TableCell sx={{ fontWeight: 'bold' }}>Имя в Git</TableCell>
                <TableCell sx={{ fontWeight: 'bold' }}>Email (Основной)</TableCell>
                <TableCell sx={{ fontWeight: 'bold', textAlign: 'center' }}>Коммиты</TableCell>
                <TableCell sx={{ fontWeight: 'bold', textAlign: 'center' }}>Текучесть (Churn)</TableCell>
                <TableCell sx={{ fontWeight: 'bold', textAlign: 'center' }}>Квалификация (PERS)</TableCell>
                <TableCell sx={{ fontWeight: 'bold' }}>Алиасы</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {team.map((c) => (
                <TableRow key={c.id} hover>
                  <TableCell>{c.displayName || 'Неизвестно'}</TableCell>
                  <TableCell>{c.primaryEmail}</TableCell>
                  <TableCell align="center">{c.totalCommits}</TableCell>
                  
                  {/* Подсветка метрики Churn: чем выше, тем хуже */}
                  <TableCell align="center">
                    <Typography color={c.churnFactor > 0.4 ? 'error' : (c.churnFactor > 0.2 ? 'warning.main' : 'success.main')}>
                      {c.churnFactor ? (c.churnFactor * 100).toFixed(1) + '%' : '0%'}
                    </Typography>
                  </TableCell>

                  {/* Подсветка коэффициента PERS: < 1.0 - это хорошо (ускоряет работу) */}
                  <TableCell align="center">
                    <Chip 
                      label={c.calculatedPersRating <= 1.0 ? 'Senior/Middle' : 'Junior/Risk'} 
                      color={c.calculatedPersRating <= 1.0 ? 'success' : 'warning'} 
                      size="small" 
                      variant="outlined"
                    />
                  </TableCell>

                  <TableCell>
                    {c.aliases && c.aliases.length > 0 ? (
                      c.aliases.map(a => <Chip key={a} label={a} size="small" sx={{ mr: 0.5, mb: 0.5 }} />)
                    ) : (
                      <Typography variant="caption" color="text.secondary">-</Typography>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Paper>
      )}

      {/* Модальное окно для слияния профилей */}
      <Dialog open={openMerge} onClose={() => setOpenMerge(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Объединение дубликатов (Aliases)</DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 3, mt: 1, color: 'text.secondary' }}>
            Если разработчик делал коммиты с разных устройств под разными email, система посчитает их за двух разных людей. Выберите профили для объединения, чтобы статистика была точной.
          </Typography>
          
          <TextField 
            select fullWidth label="Основной профиль (Останется)" 
            value={primaryId} onChange={e => setPrimaryId(e.target.value)} 
            sx={{ mb: 3 }}
          >
            {team.map(c => (
              <MenuItem key={`prim-${c.id}`} value={c.id} disabled={c.id === duplicateId}>
                {c.displayName || c.primaryEmail} ({c.primaryEmail})
              </MenuItem>
            ))}
          </TextField>

          <TextField 
            select fullWidth label="Дубликат (Будет удален, коммиты перенесены)" 
            value={duplicateId} onChange={e => setDuplicateId(e.target.value)}
          >
            {team.map(c => (
              <MenuItem key={`dup-${c.id}`} value={c.id} disabled={c.id === primaryId}>
                {c.displayName || c.primaryEmail} ({c.primaryEmail})
              </MenuItem>
            ))}
          </TextField>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setOpenMerge(false)} color="inherit" disabled={merging}>Отмена</Button>
          <Button onClick={handleMerge} variant="contained" color="error" disabled={merging || !primaryId || !duplicateId}>
            {merging ? 'Объединение...' : 'Склеить профили'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}