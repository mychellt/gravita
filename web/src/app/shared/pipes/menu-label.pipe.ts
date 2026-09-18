import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'menuLabel', standalone: true })
export class MenuLabelPipe implements PipeTransform {
  transform(menus: { key: string; label: string }[], key: () => string): string {
    const k = typeof key === 'function' ? key() : key;
    return menus.find(m => m.key === k)?.label ?? '';
  }
}
