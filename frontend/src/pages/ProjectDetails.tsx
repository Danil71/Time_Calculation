import AddIcon from '@mui/icons-material/Add';
import CalculateIcon from '@mui/icons-material/Calculate';
import DeleteIcon from '@mui/icons-material/Delete';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import SyncIcon from '@mui/icons-material/Sync';
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
  Divider,
  IconButton,
  MenuItem,
  Paper,
  Snackbar,
  TextField,
  Typography
} from '@mui/material';
import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api/axiosClient';

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

interface Estimation {
  reportId: string;
  calculatedAt: string;
  totalSloc: number;
  avgComplexity: number;
  churnRate: number;
  effortPm: number;
  durationMonths: number;
  recommendedTeam: number;
  techStack?: Record<string, number>;
}

interface ProgrammingLanguage {
  name: string;
  locPerFp: number;
}

export default function ProjectDetails() {
  const { id } = useParams<{ id: string }>(); 
  
  const [project, setProject] = useState<Project | null>(null);
  const [estimations, setEstimations] = useState<Estimation[]>([]);
  const [availableLanguages, setAvailableLanguages] = useState<ProgrammingLanguage[]>([]);
  
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [calculating, setCalculating] = useState(false);
  
  const[openModal, setOpenModal] = useState(false);
  const [fpInputs, setFpInputs] = useState<Record<string, string>>({});

  const [openCompleteModal, setOpenCompleteModal] = useState(false);
  const [actualDuration, setActualDuration] = useState('');
  const [completing, setCompleting] = useState(false);
  const [forecastChangedOpen, setForecastChangedOpen] = useState(false);

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

  const fetchLanguages = async () => {
    try {
      const res = await api.get('/languages');
      setAvailableLanguages(res.data);
    } catch (error) {
      console.error('Ошибка загрузки языков', error);
    }
  };

  useEffect(() => {
    fetchProjectData();
    fetchLanguages();
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

  const handleOpenModal = () => {
    const initialInputs: Record<string, string> = {};
    const latestEst = estimations.length > 0 ? estimations[0] : null;
    
    if (latestEst?.techStack) {
      Object.keys(latestEst.techStack).forEach(lang => {
        initialInputs[lang] = ''; 
      });
    }
    setFpInputs(initialInputs);
    setOpenModal(true);
  };

  const handleFpChange = (lang: string, value: string) => {
    setFpInputs(prev => ({ ...prev, [lang]: value }));
  };

  const removeLanguage = (lang: string) => {
    const newInputs = { ...fpInputs };
    delete newInputs[lang];
    setFpInputs(newInputs);
  };

  const addNewLanguageField = () => {
    setFpInputs(prev => ({ ...prev, ['_new_' + Date.now()]: '' }));
  };

  const handleCalculate = async () => {
    setCalculating(true);
    try {
      const cleanData: Record<string, number> = {};
      Object.entries(fpInputs).forEach(([key, val]) => {
        if (!key.startsWith('_new_') && val) {
          cleanData[key] = parseFloat(val);
        }
      });

      await api.post(`/estimations/${id}/calculate`, { targetFpDetails: cleanData });

      setOpenModal(false);
      fetchProjectData();
      setForecastChangedOpen(true);
    } catch (error) {
      console.error('Ошибка расчета COCOMO:', error);
      alert('Ошибка при расчете оценки. Убедитесь, что код был проанализирован (Синхронизация с Git).');
    } finally {
      setCalculating(false);
    }
  };

  const handleCompleteProject = async () => {
    setCompleting(true);
    try {
      await api.put(`/projects/${id}/complete?actualDurationMonths=${actualDuration}`);
      setOpenCompleteModal(false);
      alert('Проект успешно завершен! Данные сохранены для обучения ML-модели.');
      fetchProjectData(); // Обновляем статус
    } catch (error) {
      console.error('Ошибка завершения:', error);
      alert('Не удалось завершить проект.');
    } finally {
      setCompleting(false);
    }
  };

  const handleDownloadPdf = async () => {
    if (!latestEst) return;
    try {
      // Важно: responseType: 'blob' заставляет axios не парсить ответ как JSON, а читать как файл
      const response = await api.get(`/estimations/${latestEst.reportId}/pdf`, {
        responseType: 'blob',
      });
      
      // Создаем виртуальную ссылку и эмулируем клик для скачивания
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `Estimation_Report_${project?.name}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (error) {
      console.error('Ошибка при скачивании PDF:', error);
      alert('Не удалось скачать отчет.');
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
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
        <Box>
          <Typography variant="h4" fontWeight="bold" sx={{ display: 'inline-block', mr: 2 }}>
            {project.name}
          </Typography>
          <Chip 
            label={project.status === 'COMPLETED' ? 'ЗАВЕРШЕН' : 'В РАЗРАБОТКЕ'} 
            color={project.status === 'COMPLETED' ? 'success' : 'primary'} 
            size="small" 
          />
        </Box>

        <Box sx={{ display: 'flex', gap: 2 }}>
          <Button 
            variant="outlined" 
            color="inherit"
            component={Link} 
            to={`/project/${id}/team`}
          >
            Команда
          </Button>

          {project.status !== 'COMPLETED' && (
            <>
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
                onClick={handleOpenModal}
              >
                Рассчитать сроки
              </Button>
              <Button 
                variant="contained" 
                color="success" 
                onClick={() => setOpenCompleteModal(true)}
              >
                Завершить
              </Button>
            </>
          )}
        </Box>
      </Box>

      {latestEst ? (
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '2fr 1fr' }, gap: 3 }}>
          <Box>
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
            
            <Paper sx={{ p: 3, mt: 3 }}>
              <Button 
                  variant="outlined" 
                  color="error"
                  size="small"
                  startIcon={<PictureAsPdfIcon />}
                  onClick={handleDownloadPdf}
                >
                  Скачать PDF
              </Button>
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

      <Dialog open={openModal} onClose={() => setOpenModal(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Настройка прогноза (Функциональные точки)</DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 3, mt: 1, color: 'text.secondary' }}>
            Введите ожидаемое количество Функциональных Точек для каждого языка. Система переведет их в строки кода автоматически. Оставьте поля пустыми для оценки текущего кода.
          </Typography>

          {Object.entries(fpInputs).map(([langKey, fpValue]) => {
            const isNew = langKey.startsWith('_new_');
            return (
              <Box key={langKey} sx={{ display: 'flex', gap: 2, mb: 2, alignItems: 'center' }}>
                {isNew ? (
                  <TextField
                    select fullWidth size="small" label="Выберите язык"
                    value=""
                    onChange={(e) => {
                      const selectedLang = e.target.value;
                      const newInputs = { ...fpInputs };
                      delete newInputs[langKey];
                      newInputs[selectedLang] = '';
                      setFpInputs(newInputs);
                    }}
                  >
                    {availableLanguages
                      .filter(l => !Object.keys(fpInputs).includes(l.name))
                      .map(l => (
                        <MenuItem key={l.name} value={l.name}>{l.name} (1 FP = {l.locPerFp} LOC)</MenuItem>
                    ))}
                  </TextField>
                ) : (
                  <Typography sx={{ width: '40%', fontWeight: 'bold' }}>{langKey}</Typography>
                )}

                <TextField
                  fullWidth size="small" type="number" label="Кол-во Функциональных точек"
                  value={fpValue}
                  onChange={(e) => handleFpChange(langKey, e.target.value)}
                  disabled={isNew}
                />

                <IconButton color="error" onClick={() => removeLanguage(langKey)}>
                  <DeleteIcon />
                </IconButton>
              </Box>
            );
          })}

          <Button startIcon={<AddIcon />} onClick={addNewLanguageField} sx={{ mt: 1 }}>
            Добавить технологию
          </Button>

        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setOpenModal(false)} color="inherit">Отмена</Button>
          <Button onClick={handleCalculate} variant="contained" disabled={calculating}>
            {calculating ? 'Вычисление...' : 'Рассчитать прогноз'}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={forecastChangedOpen}
        autoHideDuration={5000}
        onClose={() => setForecastChangedOpen(false)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert
          onClose={() => setForecastChangedOpen(false)}
          severity="success"
          variant="filled"
          sx={{ width: '100%' }}
        >
          Текущий прогноз изменился.
        </Alert>
      </Snackbar>

      <Dialog open={openCompleteModal} onClose={() => setOpenCompleteModal(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Завершение проекта</DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 3, mt: 1, color: 'text.secondary' }}>
            Укажите фактическое время, которое было затрачено на разработку этого проекта (от старта до релиза). Эти данные будут использованы нейросетью для повышения точности будущих прогнозов.
          </Typography>
          <TextField
            fullWidth
            label="Фактическое время (в месяцах)"
            type="number"
            placeholder="Например: 6.5"
            value={actualDuration}
            onChange={(e) => setActualDuration(e.target.value)}
          />
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setOpenCompleteModal(false)} color="inherit">Отмена</Button>
          <Button onClick={handleCompleteProject} variant="contained" color="success" disabled={completing || !actualDuration}>
            {completing ? 'Сохранение...' : 'Подтвердить завершение'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}