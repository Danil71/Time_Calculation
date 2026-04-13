import DeleteIcon from '@mui/icons-material/Delete';
import GitHubIcon from '@mui/icons-material/GitHub';
import { Alert, Box, Button, Card, CardActions, CardContent, Chip, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle, IconButton, Typography } from '@mui/material';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/axiosClient';

interface Project {
  id: string;
  name: string;
  repoUrl: string;
  branchName: string;
  status: string;
}

export default function Dashboard() {
  const [deleteModalOpen, setDeleteDialogOpen] = useState(false);
  const [projectToDelete, setProjectToDelete] = useState<string | null>(null);
  const[deleting, setDeleting] = useState(false);
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchProjects();
  },[]);

  const handleDeleteClick = (id: string) => {
    setProjectToDelete(id);
    setDeleteDialogOpen(true);
  };

  const handleConfirmDelete = async () => {
    if (!projectToDelete) return;
    setDeleting(true);
    try {
      await api.delete(`/projects/${projectToDelete}`);
      setDeleteDialogOpen(false);
      setProjectToDelete(null);
      fetchProjects(); // Обновляем список после удаления
    } catch (err) {
      console.error('Ошибка удаления', err);
      alert('Не удалось удалить проект.');
    } finally {
      setDeleting(false);
    }
  };

  const fetchProjects = async () => {
    try {
      // Axios сам подставит токен из localStorage благодаря нашему перехватчику
      const response = await api.get('/projects');
      setProjects(response.data);
    } catch (err) {
      console.error('Ошибка загрузки проектов', err);
      setError('Не удалось загрузить проекты. Возможно, проблема с доступом.');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <Box sx={{ display: 'flex', justifyContent: 'center', mt: 10 }}><CircularProgress /></Box>;
  }

  return (
    <Box sx={{ mt: 4 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 'bold' }}>Дашборд проектов</Typography>
        <Button variant="contained" color="primary" component={Link} to="/add">
          + Новый проект
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      {projects.length === 0 && !error ? (
        <Typography variant="body1" color="text.secondary" sx={{ textAlign: 'center', mt: 5 }}>
          У вас пока нет ни одного проекта. Нажмите «Новый проект», чтобы начать.
        </Typography>
      ) : (
        <Box sx={{ 
          display: 'grid', 
          gridTemplateColumns: { xs: '1fr', md: '1fr 1fr', lg: 'repeat(3, 1fr)' }, 
          gap: 3 
        }}>
          {projects.map((project) => (
            <Card key={project.id} sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
              <CardContent sx={{ flexGrow: 1 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                  <Typography variant="h6" noWrap title={project.name}>
                    {project.name}
                  </Typography>
                  <Chip label={project.status} size="small" color={project.status === 'ACTIVE' ? 'success' : 'default'} />
                  <IconButton 
                    color="error" 
                    size="small" 
                    onClick={() => handleDeleteClick(project.id)}
                    title="Удалить проект"
                  >
                    <DeleteIcon />
                  </IconButton>
                </Box>
                
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 1, color: 'text.secondary' }}>
                  <GitHubIcon sx={{ mr: 1, fontSize: 20 }} />
                  <Typography variant="body2" noWrap title={project.repoUrl}>
                    {project.repoUrl}
                  </Typography>
                </Box>
                
                <Typography variant="body2" color="text.secondary">
                  Ветка: <strong>{project.branchName}</strong>
                </Typography>
              </CardContent>
              {/* ДОБАВЛЕННЫЙ БЛОК С КНОПКОЙ */}
              <CardActions sx={{ p: 2, pt: 0 }}>
                <Button 
                  component={Link} 
                  to={`/project/${project.id}`} 
                  variant="outlined" 
                  fullWidth
                >
                  Открыть аналитику
                </Button>
              </CardActions>
            </Card>
          ))}
        </Box>
      )}
      <Dialog open={deleteModalOpen} onClose={() => setDeleteDialogOpen(false)}>
        <DialogTitle>Подтверждение удаления</DialogTitle>
        <DialogContent>
          <Typography>
            Вы уверены, что хотите удалить этот проект? 
            Вся история расчетов, профили команды и метрики будут безвозвратно удалены.
          </Typography>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setDeleteDialogOpen(false)} color="inherit" disabled={deleting}>
            Отмена
          </Button>
          <Button onClick={handleConfirmDelete} variant="contained" color="error" disabled={deleting}>
            {deleting ? 'Удаление...' : 'Удалить навсегда'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}