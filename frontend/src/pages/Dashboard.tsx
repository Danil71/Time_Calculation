import { Box, Button, Typography } from '@mui/material';
import { Link } from 'react-router-dom';

export default function Dashboard() {
  return (
    <Box sx={{ mt: 10, textAlign: 'center' }}>
      <Typography variant="h4" gutterBottom>
        Дашборд проектов
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
        Здесь будет отображаться статистика из Git и списки проектов. (В разработке)
      </Typography>
      <Button variant="contained" color="primary" component={Link} to="/add">
        Подключить новый репозиторий
      </Button>
    </Box>
  );
}