import { format } from 'date-fns';

export const getFormattedTimestamp = (timestamp: number): string | null => {
  return timestamp ? format(new Date(timestamp), 'yyyy-MM-dd HH:mm:ss xx') : null;
};