const ALLOWED_START_HOUR = 13; // 1:00 PM (13:00)
const ALLOWED_END_HOUR = 21;   // 9:00 PM (21:00)

function isWithinPermittedHours(date = new Date()) {
  const currentHour = date.getHours();
  return currentHour >= ALLOWED_START_HOUR && currentHour < ALLOWED_END_HOUR;
}

function getNextUnlockInfo(date = new Date()) {
  const currentHour = date.getHours();

  if (!isWithinPermittedHours(date)) {
    const target = new Date(date.getFullYear(), date.getMonth(), date.getDate(), ALLOWED_START_HOUR, 0, 0, 0);
    if (currentHour >= ALLOWED_END_HOUR) {
      target.setDate(target.getDate() + 1);
    }
    const secondsRemaining = Math.max(0, Math.floor((target.getTime() - date.getTime()) / 1000));
    return {
      isPermittedHour: false,
      secondsRemaining,
      targetTimeStr: '1:00 PM (13:00)',
      targetTimestamp: target.getTime(),
      reason: 'outside_window'
    };
  } else {
    const nextHour = new Date(date.getFullYear(), date.getMonth(), date.getDate(), currentHour + 1, 0, 0, 0);
    const secondsRemaining = Math.max(0, Math.floor((nextHour.getTime() - date.getTime()) / 1000));
    const targetHourStr = currentHour + 1 === ALLOWED_END_HOUR 
      ? '21:00 (Lock Time)' 
      : `${String(currentHour + 1).padStart(2, '0')}:00`;

    return {
      isPermittedHour: true,
      secondsRemaining,
      targetTimeStr: targetHourStr,
      targetTimestamp: nextHour.getTime(),
      reason: 'hourly_reset'
    };
  }
}

function formatDuration(totalSeconds) {
  const hours = Math.floor(totalSeconds / 3600);
  const mins = Math.floor((totalSeconds % 3600) / 60);
  const secs = totalSeconds % 60;
  if (hours > 0) {
    return `${hours}h ${mins}m ${secs}s`;
  }
  return `${mins}m ${secs}s`;
}

// Test scenarios:
const scenarios = [
  new Date('2026-08-15T21:20:00'), // Night after 9pm (now)
  new Date('2026-08-15T23:59:59'), // Late night
  new Date('2026-08-16T08:30:00'), // Morning before 1pm
  new Date('2026-08-16T12:59:50'), // 10s before 1pm
  new Date('2026-08-16T13:00:00'), // Exactly 1pm
  new Date('2026-08-16T15:20:00'), // Afternoon
  new Date('2026-08-16T20:55:00'), // 5m before 9pm lock
  new Date('2026-08-16T21:00:00')  // Exactly 9pm lock
];

console.log('Testing Daily Window (1PM - 9PM):');
scenarios.forEach(d => {
  const info = getNextUnlockInfo(d);
  console.log(`Time: ${d.toLocaleTimeString()} | Permitted: ${info.isPermittedHour ? 'YES ✅' : 'NO ❌'} | Next: ${info.targetTimeStr} in ${formatDuration(info.secondsRemaining)}`);
});
