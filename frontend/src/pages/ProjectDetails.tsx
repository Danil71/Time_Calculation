import CalculateIcon from '@mui/icons-material/Calculate';
import SyncIcon from '@mui/icons-material/Sync';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Paper,
  TextField,
  Typography
} from '@mui/material';
import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { api } from '../api/axiosClient';

// Импорты для графиков
import { ArcElement, Chart as ChartJS, Legend, Tooltip } from 'chart.js';
import { Doughnut } from 'react-chartjs-2';

ChartJS.register(ArcElement, Tooltip, Legend);

interface Project {
  id: string;
  name: string;
  repoUrl: string;
  branchName: string;
  status: string;
}

// Описание отчета
interface Estimation {
  reportId: string;
  calculatedAt: string;
  totalSloc: number;
  avgComplexity: number;
  churnRate: number;
  effortPm: number;
  durationMonths: number;
  recommendedTeam: number;
  techStack?: Record<string, number>; // В бэкенде мы назвали это techStack
}

export default function ProjectDetails() {
  const { id } = useParams<{ id: string }>(); 
  
  const [project, setProject] = useState<Project | null>(null);
  const [estimations, setEstimations] = useState<Estimation[]>([]);
  const [loading, setLoading] = useState(true);
  
  const [syncing, setSyncing] = useState(false);
  const [calculating, setCalculating] = useState(false);
  const [openModal, setOpenModal] = useState(false);
  const[targetSloc, setTargetSloc] = useState('');

  const fetchProjectData = async () => {
    try {
      const projRes = await api.get(`/projects/${id}`);
      setProject(projRes.data);
      const estRes = await api.get(`/estimations/${id}/history`);
      setEstimations(estRes.data);
    } catch (error) {
      console.error('Ошибка загрузки данных:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProjectData();
  }, [id]);

  const handleSyncGit = async () => {
    setSyncing(true);
    try {
      await api.post(`/git/${id}/analyze`);
      alert('Анализ Git успешно завершен! Срез кода обновлен.');
      fetchProjectData(); 
    } catch (error) {
      console.error('Ошибка синхронизации Git:', error);
      alert('Ошибка при анализе Git-репозитория.');
    } finally {
      setSyncing(false);
    }
  };

  const handleCalculate = async () => {
    setCalculating(true);
    try {
      const url = targetSloc 
        ? `/estimations/${id}/calculate?targetSloc=${targetSloc}` 
        : `/estimations/${id}/calculate`;
      
      await api.post(url);
      setOpenModal(false);
      setTargetSloc('');
      fetchProjectData(); 
    } catch (error) {
      console.error('Ошибка расчета COCOMO:', error);
      alert('Ошибка при расчете оценки. Убедитесь, что код был проанализирован (Синхронизация с Git).');
    } finally {
      setCalculating(false);
    }
  };

  if (loading) return <Box sx={{ mt: 10, textAlign: 'center' }}><CircularProgress /></Box>;
  if (!project) return <Typography>Проект не найден</Typography>;

  const latestEst = estimations.length > 0 ? estimations[0] : null;

  const chartData = {
    labels: latestEst?.techStack ? Object.keys(latestEst.techStack) : ['Нет данных'],
    datasets:[{
      data: latestEst?.techStack ? Object.values(latestEst.techStack) : [1],
      backgroundColor:['#3B82F6', '#10B981', '#F59E0B', '#F43F5E', '#8B5CF6'],
      borderWidth: 0,
    }]
  };

  return (
    <Box sx={{ mt: 4 }}>
      {/* Шапка */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
        <Typography variant="h4" fontWeight="bold">{project.name}</Typography>
        <Box sx={{ display: 'flex', gap: 2 }}>
          <Button 
            variant="outlined" 
            color="secondary" 
            startIcon={syncing ? <CircularProgress size={20} /> : <SyncIcon />}
            onClick={handleSyncGit}
            disabled={syncing}
          >
            {syncing ? 'Анализ Git...' : 'Синхронизировать'}
          </Button>
          <Button 
            variant="contained" 
            color="primary" 
            startIcon={<CalculateIcon />}
            onClick={() => setOpenModal(true)}
          >
            Рассчитать сроки
          </Button>
        </Box>
      </Box>

      {/* Дашборд метрик */}
      {latestEst ? (
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '2fr 1fr' }, gap: 3 }}>
          
          {/* Левая колонка (Метрики и Отчет) */}
          <Box>
            {/* Верхний ряд метрик */}
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, 1fr)' }, gap: 3 }}>
              <Paper sx={{ p: 3, textAlign: 'center' }}>
                <Typography color="text.secondary">Текущий размер (SLOC)</Typography>
                <Typography variant="h4" color="primary" sx={{ mt: 1 }}>{latestEst.totalSloc}</Typography>
              </Paper>
              <Paper sx={{ p: 3, textAlign: 'center' }}>
                <Typography color="text.secondary">Сложность (McCabe)</Typography>
                <Typography variant="h4" color="error" sx={{ mt: 1 }}>{latestEst.avgComplexity.toFixed(1)}</Typography>
              </Paper>
              <Paper sx={{ p: 3, textAlign: 'center' }}>
                <Typography color="text.secondary">Текучесть кода (Churn)</Typography>
                <Typography variant="h4" color="warning.main" sx={{ mt: 1 }}>{(latestEst.churnRate * 100).toFixed(1)}%</Typography>
              </Paper>
            </Box>
            
            {/* Финансово-временной отчет */}
            <Paper sx={{ p: 3, mt: 3 }}>
              <Typography variant="h6" gutterBottom>Текущий прогноз (COCOMO II)</Typography>
              <Divider sx={{ mb: 2 }} />
              <Box sx={{ display: 'flex', justifyContent: 'space-around', textAlign: 'center' }}>
                <Box>
                  <Typography color="text.secondary">Трудоемкость</Typography>
                  <Typography variant="h5">{latestEst.effortPm.toFixed(1)} чел-мес</Typography>
                </Box>
                <Box>
                  <Typography color="text.secondary">Рекомендуемый срок</Typography>
                  <Typography variant="h5">{latestEst.durationMonths.toFixed(1)} месяцев</Typography>
                </Box>
                <Box>
                  <Typography color="text.secondary">Размер команды</Typography>
                  <Typography variant="h5">{Math.ceil(latestEst.recommendedTeam)} чел</Typography>
                </Box>
              </Box>
            </Paper>
          </Box>

          {/* Правая колонка (Диаграмма языков) */}
          <Paper sx={{ p: 3, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            <Typography variant="h6" gutterBottom>Стек технологий</Typography>
            <Box sx={{ width: '80%', mt: 2 }}>
              <Doughnut data={chartData} options={{ plugins: { legend: { position: 'bottom' } } }} />
            </Box>
          </Paper>
          
        </Box>
      ) : (
        <Alert severity="info" sx={{ mt: 2 }}>
          Нет данных для отображения. Нажмите «Синхронизировать», а затем «Рассчитать сроки».
        </Alert>
      )}

      {/* Модальное окно для ввода целевого размера */}
      <Dialog open={openModal} onClose={() => setOpenModal(false)}>
        <DialogTitle>Настройка прогноза</DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 3 }}>
            Если проект находится на начальной стадии, введите ожидаемый итоговый размер (в строках кода). Если поле оставить пустым, система рассчитает стоимость только того кода, который уже есть в Git.
          </Typography>
          <TextField
            fullWidth
            label="Целевой размер (Target SLOC)"
            type="number"
            placeholder="Например: 50000"
            value={targetSloc}
            onChange={(e) => setTargetSloc(e.target.value)}
          />
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setOpenModal(false)} color="inherit">Отмена</Button>
          <Button onClick={handleCalculate} variant="contained" disabled={calculating}>
            {calculating ? 'Вычисление...' : 'Рассчитать'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}