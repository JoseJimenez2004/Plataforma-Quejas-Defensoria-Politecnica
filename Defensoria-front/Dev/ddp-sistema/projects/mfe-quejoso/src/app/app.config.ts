import { ApplicationConfig, provideBrowserGlobalErrorListeners, importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
// @ts-ignore
import { LucideAngularModule } from 'lucide-angular';
// @ts-ignore
import {
  Hand, Search, Users, HelpCircle, BookOpen, GraduationCap, ClipboardList, Scale,
  FileText, Download, Check, Printer, CheckCircle, Mail, Lock, EyeOff, Eye, KeyRound,
  ThumbsUp, X, Paperclip, Trash2, Pencil, AlertTriangle, User, Folder, CirclePlus,
  Bell, Settings, Info, LockOpen, Hash
} from 'lucide-angular';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    importProvidersFrom(
      LucideAngularModule.pick({
        Hand, Search, Users, HelpCircle, BookOpen, GraduationCap, ClipboardList, Scale,
        FileText, Download, Check, Printer, CheckCircle, Mail, Lock, EyeOff, Eye, KeyRound,
        ThumbsUp, X, Paperclip, Trash2, Pencil, AlertTriangle, User, Folder, CirclePlus,
        Bell, Settings, Info, LockOpen, Hash
      })
    )
  ],
};
