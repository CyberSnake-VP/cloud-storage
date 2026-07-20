import {Box, Button, Link} from "@mui/material";import {FilePageButton} from "../components/FilePageButton/FilePageButton.jsx";
import {useAuthContext} from "../context/Auth/AuthContext.jsx";
import Typography from "@mui/material/Typography";
import {useNavigate} from "react-router-dom";

export default function Index() {

    const {auth} = useAuthContext();

    const navigate = useNavigate();

    return (
        <Box sx={{
            minHeight: '100vh',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            position: 'relative',
            px: 2
        }}>

            {/* Основной контент по центру */}
            <Box sx={{
                textAlign: 'center',
                maxWidth: '600px'
            }}>
                <Typography variant="h3" sx={{ mb: 3, fontWeight: 'bold' }}>
                    ☁️ Cloud Storage
                </Typography>

                <Typography variant="h6" sx={{ mb: 2, color: 'text.secondary' }}>
                    Многопользовательское файловое облако
                </Typography>

                <Typography variant="body1" sx={{ mb: 4, color: 'text.secondary' }}>
                    Вы можете использовать его для загрузки и хранения файлов.
                    Создавайте папки, делитесь документами, скачивайте ZIP-архивы.
                </Typography>

                <Button
                    variant="contained"
                    size="large"
                    onClick={() => navigate("/registration")}
                    sx={{ px: 6, py: 1.5, fontSize: '1.1rem' }}
                >
                    Зарегистрироваться
                </Button>

                <Typography variant="body2" sx={{ mt: 2, color: 'text.secondary' }}>
                    Уже есть аккаунт?{' '}
                    <Link sx={{ cursor: 'pointer' }} onClick={() => navigate("/login")}>
                        Войти
                    </Link>
                </Typography>
            </Box>

            {/* Ссылка на roadmap слева внизу */}
            <Typography variant="body2" sx={{
                position: 'absolute',
                bottom: 16,
                left: 16,
                color: 'text.secondary'
            }}>
                <Link href="https://zhukovsd.github.io/java-backend-learning-course/projects/cloud-file-storage/"
                      target="_blank"
                      underline="hover">
                    Java Roadmap Сергея Жукова
                </Link>
            </Typography>
        </Box>
    )
}