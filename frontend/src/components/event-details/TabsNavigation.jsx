import '../../styles/EventDetails.css';

const TABS = [
  { key: 'overview',  label: 'Overview' },
  { key: 'schedule',  label: 'Schedule' },
  { key: 'speakers',  label: 'Speakers' },
  { key: 'gallery',   label: 'Gallery' },
  { key: 'faqs',      label: 'FAQs' },
];

export default function TabsNavigation({ activeTab, onTabChange }) {
  return (
    <nav className="ed-tabs">
      <div className="ed-tabs__inner">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            className={`ed-tabs__btn ${activeTab === tab.key ? 'ed-tabs__btn--active' : ''}`}
            onClick={() => onTabChange(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>
    </nav>
  );
}
